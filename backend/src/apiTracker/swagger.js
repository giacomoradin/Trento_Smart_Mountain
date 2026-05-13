import swaggerAutogen from "swagger-autogen";

const doc = {
  info: {
    title: "Trento Smart Mountain API",
    description: "REST API for managing users, hiking groups and IoT telemetry",
    version: "1.0.0",
  },
  host: "localhost:3000",
  basePath: "/",
  schemes: ["http"],
  securityDefinitions: {
    bearerAuth: {
      type: "apiKey",
      name: "Authorization",
      in: "header",
      description: "Enter JWT token WITH the 'Bearer ' prefix. Example: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
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