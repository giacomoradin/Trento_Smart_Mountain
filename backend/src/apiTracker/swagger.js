import swaggerAutogen from "swagger-autogen";

const doc = {
  info: {
    title: "Trento Smart Mountain API",
    description: "REST API for managing users, hiking groups and IoT telemetry",
  },
  host: "localhost:3000",
  basePath: "/",
};

const outputFile = "../apiTracker/swagger-output.json";
const routes = ["./src/app.js"];

swaggerAutogen()(outputFile, routes, doc);
