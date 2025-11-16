# Read Me First
The following was discovered as part of building this project:

* The original package name 'eu.companon.clarity-walk-core' is invalid and this project uses 'eu.companon.clarity_walk_core' instead.

# Getting Started

### Reference Documentation
For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/3.5.7/maven-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/3.5.7/maven-plugin/build-image.html)
* [Spring Web](https://docs.spring.io/spring-boot/3.5.7/reference/web/servlet.html)

### Guides
The following guides illustrate how to use some features concretely:

* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)

### Maven Parent overrides

Due to Maven's design, elements are inherited from the parent POM to the project POM.
While most of the inheritance is fine, it also inherits unwanted elements like `<license>` and `<developers>` from the parent.
To prevent this, the project POM contains empty overrides for these elements.
If you manually switch to a different parent and actually want the inheritance, you need to remove those overrides.

### Useful commands
curl -s http://localhost:8080/actuator | jq

# Create a message (POST) — returns 201 with Location header and JSON body containing the UUID id
curl -i -X POST http://localhost:8080/messages \
-H "Content-Type: application/json" \
-d '{"content":"Hello from curl"}'

# Create and extract id from JSON response (requires jq)
ID=$(curl -s -X POST http://localhost:8080/messages \
-H "Content-Type: application/json" \
-d '{"content":"Hello from curl"}' | jq -r '.id')
echo "created id: $ID"

# Alternatively extract Location header (no jq)
LOCATION=$(curl -s -i -X POST http://localhost:8080/messages \
-H "Content-Type: application/json" \
-d '{"content":"Hello from curl"}' | awk '/^Location:/ {print $2}' | tr -d '\r')
echo "location: $LOCATION"

# Get a single message by id (replace <id> with the UUID from response)
curl -i http://localhost:8080/messages/<id>

# Example using the ID variable from above
curl -i http://localhost:8080/messages/$ID

# List all messages
curl -i http://localhost:8080/messages