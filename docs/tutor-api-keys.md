# Tutor API-key storage and rotation

Each tutor has one encrypted API key shared by their agents. The `/settings/api-key` page can save, replace, or remove it. Status means **stored**, not provider-verified. Agent provider integration is deferred to story 6.

## Configure encryption

Supply these backend environment variables using your deployment secret manager:

- `TUTOR_CREDENTIAL_ACTIVE_KEY_ID`: a nonsecret identifier such as `key-2026-09` (letters, digits, underscores, or hyphens; up to 100 characters).
- `TUTOR_CREDENTIAL_KEYS_JSON`: a JSON object mapping each retained key ID to a Base64-encoded random 32-byte encryption key.

Example structure (the placeholder is not a usable key):

```text
TUTOR_CREDENTIAL_ACTIVE_KEY_ID=key-2026-09
TUTOR_CREDENTIAL_KEYS_JSON={"key-2026-09":"<Base64-encoded random 32-byte key>"}
```

For local development, generate the key using `openssl rand -base64 32`, then store it in an untracked, permission-restricted backend environment file or local secret store. `yaply-core/.env` is imported by Spring as properties: use the raw JSON object there, without shell-style surrounding quotes. When exporting JSON in a shell, use single quotes around the JSON to protect its syntax. Do not commit the generated value, paste it into logs, or use a Vite variable.

The existing Compose configurations pass both variables only to the backend. Deploy with HTTPS. Keep keys stable across restarts and identical across backend instances. The encryption secret must be independent of `JWT_SECRET` and `OPENAI_API_KEY`.

With both variables absent, the application starts and permits status reads/removal, but saves and decryption fail closed. Partial or malformed configuration prevents startup. Existing keys are never automatically copied from the application's OpenAI key.

Storage uses AES-256-GCM, a fresh 12-byte nonce per save, a 128-bit authentication tag, and authenticated profile identity. The database holds ciphertext and its encryption key ID, not plaintext. Back up database records and encryption keys separately with appropriate access controls. Losing a required encryption key makes its records unreadable; users must replace their API keys if the key cannot be recovered.

## Rotate encryption keys

Rotation re-encrypts existing API keys; it does not replace the API keys at the provider.

1. Generate a new encryption key and add it under a new ID to the key ring on **all** instances, retaining every old key. Keep the old active ID during this rollout.
2. Change the active ID to the new ID on all instances. Wait for rollout completion before rotating, so older instances cannot keep creating records under the old ID.
3. Build the backend with `mvn package -DskipTests` after tests have passed. In an operator shell with database settings and the full encryption key ring already supplied, run:

   ```bash
   java -jar target/yaply-core-0.0.1-SNAPSHOT.jar \
     --app.credentials.rotate=true \
     --server.address=127.0.0.1 --server.port=0
   ```

   This starts an isolated application process bound to an ephemeral loopback port, performs rotation, prints counts, and closes. It exposes no rotation HTTP endpoint. Do not pass secrets on the command line. Run from `yaply-core` if relying on its `.env` import.
4. The command processes each tutor in a separate transaction, taking the same owner lock used by Save and Remove. Already rotated and removed records are skipped. Failed records retain their previous contents. A partial failure prints a failure count and exits unsuccessfully; keep old keys available, fix the issue, and rerun.
5. Check remaining references using your normal database administration connection:

   ```sql
   SELECT encryption_key_id, count(*)
   FROM tutor_api_credential
   GROUP BY encryption_key_id;
   ```

6. Retire an old key only after no live records reference it and no instances still write with that ID. Retain the appropriate keys for historical backups for as long as those backups must remain recoverable.

Rotation preserves the user-facing last-change timestamp. Simultaneous save/remove cannot be overwritten by an earlier rotation read because each operation re-reads under the same owner lock.

## Troubleshooting and boundaries

- “Storage unavailable”: check that encryption settings are present and valid on each instance.
- “Saved API key cannot be read”: restore the referenced encryption key or ask the tutor to replace the saved API key. Never enable an application-key fallback.
- A successful save does not establish quota, billing, model permission, or key validity. Future provider operations must report those separately and sanitize provider errors.
- Removing a stored key prevents subsequent resolution. Requests already holding a key and established realtime sessions cannot be retracted by deleting the database row.
- Do not enable HTTP body/header tracing for credential endpoints, bind credential entities into responses, or send decrypted keys to job queues. Jobs should carry agent/owner IDs and resolve credentials immediately before provider use.
