# OAuth 2.0 Setup Instructions

## Backend Configuration

1. **Update environment variables** in `.env` (create if doesn't exist):

```bash
# JWT Secret (already configured)
JWT_SECRET=your-256-bit-secret-key-change-this-in-production

# Google OAuth
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
GOOGLE_REDIRECT_URI=http://localhost:8080/auth/oauth2/callback/google

# Facebook OAuth
FACEBOOK_CLIENT_ID=your_facebook_app_id
FACEBOOK_CLIENT_SECRET=your_facebook_app_secret
FACEBOOK_REDIRECT_URI=http://localhost:8080/auth/oauth2/callback/facebook
```

## Frontend Configuration

2. **Create `.env` file** in `clarity-walk-web/clarity-walk-web/`:

```bash
VITE_GOOGLE_CLIENT_ID=your_google_client_id
VITE_FACEBOOK_CLIENT_ID=your_facebook_app_id
```

## Obtaining OAuth Credentials

### Google OAuth Setup

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing one
3. Navigate to **APIs & Services** > **Credentials**
4. Click **Create Credentials** > **OAuth 2.0 Client ID**
5. Configure consent screen if prompted
6. Select **Web application** as application type
7. Add authorized redirect URI: `http://localhost:8080/auth/oauth2/callback/google`
8. Copy **Client ID** and **Client Secret**

### Facebook OAuth Setup

1. Go to [Facebook Developers](https://developers.facebook.com/)
2. Create a new app or select existing one
3. Add **Facebook Login** product to your app
4. Navigate to **Settings** > **Basic**
5. Copy **App ID** and **App Secret**
6. Navigate to **Facebook Login** > **Settings**
7. Add OAuth redirect URI: `http://localhost:8080/auth/oauth2/callback/google`
8. Enable the app for public use when ready

## Running the Application

1. **Start PostgreSQL database**:
   ```bash
   docker-compose up -d
   ```

2. **Start backend**:
   ```bash
   cd clarity-walk-core
   ./mvnw spring-boot:run
   ```

3. **Start frontend**:
   ```bash
   cd clarity-walk-web/clarity-walk-web
   npm run dev
   ```

4. Open browser to `http://localhost:5173`

## Testing OAuth Flow

1. Click "Continue with Google" or "Continue with Facebook"
2. Complete OAuth authorization in the provider's popup/redirect
3. You will be redirected back to the app with a JWT token
4. The token is stored in localStorage
5. You should now see the main Clarity Walk app interface

## Troubleshooting

- **"Client ID not configured"**: Make sure `.env` files are created and contain valid OAuth credentials
- **"Redirect URI mismatch"**: Ensure the redirect URIs in your OAuth app settings match exactly
- **CORS errors**: Check that backend CORS configuration allows `http://localhost:5173`
- **"Authentication failed"**: Check backend logs for detailed error messages
