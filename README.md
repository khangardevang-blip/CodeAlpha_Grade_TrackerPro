# Student Grade Tracker Pro 🎓

A full-stack, cloud-ready web application designed to calculate, track, and manage student grades. Built with a sleek, modern UI, it features secure user authentication, a historical dashboard, and automated PDF report generation.

## ✨ Features

* **Advanced Grade Calculation**: Automatically calculates total marks, percentage, highest/lowest scores, and assigns a final letter grade.
* **Secure Authentication**: Built-in user registration and login system using secure token authentication.
* **Personal Dashboard**: Users can view their entire grade history in a beautiful, expandable accordion layout.
* **PDF Export**: Generate and download professional PDF reports of your grade history instantly.
* **Database Ready**: Fully supports MySQL for persistent data storage, with a built-in memory fallback for quick local testing.
* **Cloud Deployable**: Includes a pre-configured `Dockerfile` for seamless deployment to platforms like Render or Railway.

## 🛠️ Tech Stack

**Frontend:**
* HTML5 / CSS3 (Custom Glassmorphism UI)
* Vanilla JavaScript
* jsPDF & autoTable (For PDF Generation)

**Backend:**
* Java 17 (Using native `com.sun.net.httpserver`)
* MySQL (via JDBC)
* Docker

## 🚀 Local Setup

### Prerequisites
* Java Development Kit (JDK 17 or higher)
* (Optional) MySQL Server for persistent data

### Running the Backend
1. Open a terminal and navigate to the `backend` directory.
2. Compile the server:
   ```bash
   javac GradeServer.java
   ```
3. Run the server:
   ```bash
   java GradeServer
   ```
   *The server will start on `http://localhost:8080`. By default, it runs in "In-Memory" mode. To use MySQL, set a `DATABASE_URL` environment variable before running.*

### Running the Frontend
1. Open a new terminal and navigate to the `frontend` directory.
2. Start a simple local server (e.g., using Python):
   ```bash
   python -m http.server 3000
   ```
3. Open your browser and go to `http://localhost:3000`.

## ☁️ Deployment

* **Frontend**: Deploy the `frontend` folder directly to [Vercel](https://vercel.com/) or [Netlify](https://www.netlify.com/). (Ensure you update the API URLs in `app.js` to point to your live backend!).
* **Backend**: Deploy the `backend` folder to [Render](https://render.com/) or [Railway](https://railway.app/). The included `Dockerfile` will handle the build process automatically. Add your MySQL string as a `DATABASE_URL` environment variable in your cloud dashboard.

## 📝 License
This project is open-source and available for educational use.
