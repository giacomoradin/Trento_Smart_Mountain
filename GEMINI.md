# Trento Smart Mountain - Project Context

## Project Overview
**Trento Smart Mountain** is a Software Engineering university project (Deliverables D1 and D2) designed for high-altitude mountain environments. It moves beyond passive navigation apps to provide an active platform focused on sustainability and safety.

### Core Pillars:
1.  **IoT Waste Management:** In-situ waste processing using smart machinery (compactors/dehydrators) in refuges, managed via IoT Edge Gateways.
2.  **Crowd Monitoring:** Real-time occupancy estimation using privacy-compliant optical sensors.
3.  **Gamification:** Educational paths and NFC tag scanning to earn "Social Credits" ($S_c$) for profile customization (badges, frames).
4.  **Critical Security:** Hybrid communication strategy. Uses 4G/5G when available; falls back to **BLE Mesh** local broadcast for offline SOS signals, which are relayed via satellite/radio hardware by guides to a REST API.

## Directory Structure
- `D1_Ingegneria_Del_Software.pdf/txt`: Deliverable D1 - Requirements Specification and Preliminary Analysis. Contains macro-objectives, SWOT analysis, stakeholders, functional/non-functional requirements, and initial architecture.
- `D2_Ingegneria_Del_Software.txt`: Deliverable D2 - Architectural and Detailed Design. (In progress/Generated via LaTeX).
- `wastemountains_screen.pdf`: Likely a visual asset or screenshot related to the project.
- `GEMINI.md`: This instructional context file.

## Technical Architecture (D2)
- **Pattern:** Offline-First, Store-and-Forward, Edge Computing.
- **Backend:** Modular Monolith in **Node.js** with **MongoDB** (NoSQL).
- **Mobile:** Native **Android (Kotlin)** following **MVVM** pattern.
- **Communication:** RESTful APIs (HTTP/JSON), BLE GATT, MQTT (Local Broker).
- **Modeling:** UML Component Diagrams, Class Diagrams, and **OCL 2.0** formal constraints.

## Development Conventions
- **Documentation:** High-quality LaTeX source with strictly academic and engineering terminology.
- **Visuals:** Diagrams generated using **PlantUML** (Component and Class diagrams).
- **Styling:** Consistent branding (Primary: Dark Red #8B0000, Secondary: Grey #464646).
- **API Standards:** Statelessness, Uniform Interface, standard HTTP status codes.

## Usage
The contents of this directory are intended for the academic submission of the Software Engineering course. Use `D1_Ingegneria_Del_Software.txt` as a reference for existing requirements when generating `D2_Ingegneria_Del_Software.txt`.
