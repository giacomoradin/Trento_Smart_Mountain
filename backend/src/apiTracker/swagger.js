import swaggerAutogen from "swagger-autogen";

const doc = {
  info: {
    title: "Trento Smart Mountain API",
    description: "REST API for managing users, hiking groups and IoT telemetry",
  },
  host: "localhost:3000",
  basePath: "/",
  schemes: ["http"],
  components: {
    securitySchemes: {
      bearerAuth: {
        type: "http",
        scheme: "bearer",
        bearerFormat: "JWT",
        description: "Enter JWT token. Example: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
      }
    }
  },
  security: [
    {
      bearerAuth: []
    }
  ]
};

const outputFile = "../apiTracker/swagger-output.json";
const routes = ["./backend/src/app.js"];

swaggerAutogen()(outputFile, routes, doc);