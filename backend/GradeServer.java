import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.File;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.security.MessageDigest;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

public class GradeServer {

    // Simple in-memory storage for fallback mode
    private static Map<String, Integer> tokenToUserId = new HashMap<>();
    private static int nextUserId = 1;
    
    private static class FallbackUser {
        int id;
        String username;
        String passwordHash;
        String token;
    }
    private static Map<String, FallbackUser> usernameToUser = new HashMap<>();

    private static class FallbackGrade {
        int userId;
        String rollNumber;
        int totalSubjects;
        double totalMarks;
        double averagePercentage;
        String grade;
        double averageScore;
        double highestScore;
        double lowestScore;
        String subjectBreakdown;
    }
    private static java.util.List<FallbackGrade> fallbackGrades = new java.util.ArrayList<>();

    private static Connection getConnection() throws SQLException {
        String dbUrl = System.getenv("DATABASE_URL");
        if (dbUrl == null || dbUrl.isEmpty()) {
            return null;
        }
        return DriverManager.getConnection(dbUrl);
    }

    private static void initDatabase() {
        try (Connection conn = getConnection()) {
            if (conn != null) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                            "id SERIAL PRIMARY KEY, " +
                            "username VARCHAR(255) UNIQUE, " +
                            "password_hash VARCHAR(255), " +
                            "token VARCHAR(255))");

                    stmt.execute("CREATE TABLE IF NOT EXISTS grades (" +
                            "id SERIAL PRIMARY KEY, " +
                            "user_id INT, " +
                            "roll_number VARCHAR(255), " +
                            "total_subjects INT, " +
                            "total_marks DOUBLE PRECISION, " +
                            "average_percentage DOUBLE PRECISION, " +
                            "grade VARCHAR(10), " +
                            "average_score DOUBLE PRECISION, " +
                            "highest_score DOUBLE PRECISION, " +
                            "lowest_score DOUBLE PRECISION, " +
                            "subject_breakdown TEXT)");
                }
                System.out.println("Database initialized.");
            } else {
                System.out.println("No DB configured, using local fallback mode.");
            }
        } catch (SQLException e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws IOException {
        initDatabase();
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/auth/register", new AuthRegisterHandler());
        server.createContext("/api/auth/login", new AuthLoginHandler());
        server.createContext("/api/grades/calculate", new GradeHandler());
        server.createContext("/api/grades/history", new HistoryHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
        System.out.println("Server started on port " + port);
    }

    // --- Utility Methods ---

    private static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String extractJsonValue(String json, String key) {
        int keyIndex = json.indexOf(key);
        if (keyIndex == -1) return "";
        int colonIndex = json.indexOf(":", keyIndex);
        int startQuote = json.indexOf("\"", colonIndex);
        if (startQuote == -1) return "";
        int endQuote = json.indexOf("\"", startQuote + 1);
        if (endQuote == -1) return "";
        return json.substring(startQuote + 1, endQuote);
    }

    private static double extractJsonNumber(String json, String key) {
        int keyIndex = json.indexOf(key);
        if (keyIndex == -1) return 0.0;
        int colonIndex = json.indexOf(":", keyIndex);
        int end = json.length();
        int commaIndex = json.indexOf(",", colonIndex);
        int braceIndex = json.indexOf("}", colonIndex);
        if (commaIndex != -1 && braceIndex != -1) end = Math.min(commaIndex, braceIndex);
        else if (commaIndex != -1) end = commaIndex;
        else if (braceIndex != -1) end = braceIndex;
        String numStr = json.substring(colonIndex + 1, end).trim();
        try { return Double.parseDouble(numStr); } catch (NumberFormatException e) { return 0.0; }
    }

    private static void setCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type,Authorization");
    }

    private static void sendJsonResponse(HttpExchange exchange, int statusCode, String jsonResponse) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(responseBytes);
        os.close();
    }

    private static Integer authenticate(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        String token = authHeader.substring(7);

        try (Connection conn = getConnection()) {
            if (conn != null) {
                try (PreparedStatement pstmt = conn.prepareStatement("SELECT id FROM users WHERE token = ?")) {
                    pstmt.setString(1, token);
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) return rs.getInt("id");
                }
            } else {
                return tokenToUserId.get(token);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    // --- Handlers ---

    static class AuthRegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String username = extractJsonValue(body, "\"username\"");
                String password = extractJsonValue(body, "\"password\"");
                
                if (username.isEmpty() || password.isEmpty()) {
                    sendJsonResponse(exchange, 400, "{\"error\":\"Invalid input\"}");
                    return;
                }
                
                String hashedPw = hashPassword(password);
                String token = UUID.randomUUID().toString();

                try (Connection conn = getConnection()) {
                    if (conn != null) {
                        String sql = "INSERT INTO users (username, password_hash, token) VALUES (?, ?, ?)";
                        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                            pstmt.setString(1, username);
                            pstmt.setString(2, hashedPw);
                            pstmt.setString(3, token);
                            pstmt.executeUpdate();
                        }
                    } else {
                        // fallback
                        if (usernameToUser.containsKey(username)) {
                            sendJsonResponse(exchange, 400, "{\"error\":\"Username taken\"}");
                            return;
                        }
                        int uId = nextUserId++;
                        FallbackUser u = new FallbackUser();
                        u.id = uId;
                        u.username = username;
                        u.passwordHash = hashedPw;
                        u.token = token;
                        usernameToUser.put(username, u);
                        tokenToUserId.put(token, uId);
                    }
                    sendJsonResponse(exchange, 200, "{\"token\":\"" + token + "\"}");
                } catch (Exception e) {
                    e.printStackTrace();
                    sendJsonResponse(exchange, 500, "{\"error\":\"Username taken or server error\"}");
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
            }
        }
    }

    static class AuthLoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String username = extractJsonValue(body, "\"username\"");
                String password = extractJsonValue(body, "\"password\"");
                String hashedPw = hashPassword(password);

                try (Connection conn = getConnection()) {
                    if (conn != null) {
                        try (PreparedStatement pstmt = conn.prepareStatement("SELECT id FROM users WHERE username = ? AND password_hash = ?")) {
                            pstmt.setString(1, username);
                            pstmt.setString(2, hashedPw);
                            ResultSet rs = pstmt.executeQuery();
                            if (rs.next()) {
                                int userId = rs.getInt("id");
                                String token = UUID.randomUUID().toString();
                                try (PreparedStatement update = conn.prepareStatement("UPDATE users SET token = ? WHERE id = ?")) {
                                    update.setString(1, token);
                                    update.setInt(2, userId);
                                    update.executeUpdate();
                                }
                                sendJsonResponse(exchange, 200, "{\"token\":\"" + token + "\"}");
                                return;
                            }
                        }
                    } else {
                        // In fallback mode, there is no real persistence for users
                        FallbackUser u = usernameToUser.get(username);
                        if (u != null && u.passwordHash.equals(hashedPw)) {
                            String token = UUID.randomUUID().toString();
                            tokenToUserId.remove(u.token);
                            u.token = token;
                            tokenToUserId.put(token, u.id);
                            sendJsonResponse(exchange, 200, "{\"token\":\"" + token + "\"}");
                            return;
                        }
                    }
                    sendJsonResponse(exchange, 401, "{\"error\":\"Invalid credentials\"}");
                } catch (Exception e) {
                    e.printStackTrace();
                    sendJsonResponse(exchange, 500, "{\"error\":\"Server error\"}");
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
            }
        }
    }

    static class GradeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Integer userId = authenticate(exchange);
                if (userId == null) {
                    sendJsonResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                    return;
                }

                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String rollNumber = extractJsonValue(body, "\"rollNumber\"");
                int totalSubjects = (int) extractJsonNumber(body, "\"totalSubjects\"");
                
                double totalMarks = 0;
                int actualSubjects = 0;
                double highestScore = Double.MIN_VALUE;
                double lowestScore = Double.MAX_VALUE;
                
                String subjectsStr = body.substring(body.indexOf("\"subjects\":"));
                String[] parts = subjectsStr.split("\\{");
                for (int i = 1; i < parts.length; i++) {
                    if (parts[i].contains("\"marks\"")) {
                        double marks = extractJsonNumber("{" + parts[i], "\"marks\"");
                        totalMarks += marks;
                        actualSubjects++;
                        if (marks > highestScore) highestScore = marks;
                        if (marks < lowestScore) lowestScore = marks;
                    }
                }

                if (actualSubjects == 0) {
                    actualSubjects = totalSubjects > 0 ? totalSubjects : 1;
                    highestScore = 0;
                    lowestScore = 0;
                }

                double averageScore = totalMarks / actualSubjects;
                double maxMarks = actualSubjects * 100.0;
                double averagePercentage = (totalMarks / maxMarks) * 100.0;

                String finalGrade = getGrade(averagePercentage);

                StringBuilder subjectsJsonBuilder = new StringBuilder("[");
                StringBuilder breakdownStr = new StringBuilder();
                for (int i = 1; i < parts.length; i++) {
                    if (parts[i].contains("\"marks\"")) {
                        String subName = extractJsonValue("{" + parts[i], "\"name\"");
                        if (subName.isEmpty()) subName = "Subject " + i;
                        double marks = extractJsonNumber("{" + parts[i], "\"marks\"");
                        String subGrade = getGrade(marks);
                        
                        subjectsJsonBuilder.append(String.format("{\"name\":\"%s\",\"marks\":%.2f,\"grade\":\"%s\"}", subName, marks, subGrade));
                        if (i < parts.length - 1) subjectsJsonBuilder.append(",");
                        
                        breakdownStr.append(subName).append(": ").append(marks).append(" (").append(subGrade).append(") | ");
                    }
                }
                if (subjectsJsonBuilder.length() > 1 && subjectsJsonBuilder.charAt(subjectsJsonBuilder.length() - 1) == ',') {
                    subjectsJsonBuilder.deleteCharAt(subjectsJsonBuilder.length() - 1);
                }
                subjectsJsonBuilder.append("]");
                
                if (breakdownStr.length() > 3) breakdownStr.setLength(breakdownStr.length() - 3);

                try (Connection conn = getConnection()) {
                    if (conn != null) {
                        String sql = "INSERT INTO grades (user_id, roll_number, total_subjects, total_marks, average_percentage, grade, average_score, highest_score, lowest_score, subject_breakdown) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                            pstmt.setInt(1, userId);
                            pstmt.setString(2, rollNumber);
                            pstmt.setInt(3, actualSubjects);
                            pstmt.setDouble(4, totalMarks);
                            pstmt.setDouble(5, averagePercentage);
                            pstmt.setString(6, finalGrade);
                            pstmt.setDouble(7, averageScore);
                            pstmt.setDouble(8, highestScore);
                            pstmt.setDouble(9, lowestScore);
                            pstmt.setString(10, breakdownStr.toString());
                            pstmt.executeUpdate();
                        }
                    } else {
                        FallbackGrade fg = new FallbackGrade();
                        fg.userId = userId;
                        fg.rollNumber = rollNumber;
                        fg.totalSubjects = actualSubjects;
                        fg.totalMarks = totalMarks;
                        fg.averagePercentage = averagePercentage;
                        fg.grade = finalGrade;
                        fg.averageScore = averageScore;
                        fg.highestScore = highestScore;
                        fg.lowestScore = lowestScore;
                        fg.subjectBreakdown = breakdownStr.toString();
                        fallbackGrades.add(fg);
                    }
                } catch (Exception e) { e.printStackTrace(); }

                String response = String.format("{\"rollNumber\":\"%s\",\"totalMarks\":%.2f,\"averagePercentage\":%.2f,\"averageScore\":%.2f,\"highestScore\":%.2f,\"lowestScore\":%.2f,\"grade\":\"%s\",\"subjects\":%s}",
                        rollNumber, totalMarks, averagePercentage, averageScore, highestScore, lowestScore, finalGrade, subjectsJsonBuilder.toString());
                sendJsonResponse(exchange, 200, response);
            } else {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
            }
        }

        private String getGrade(double marks) {
            if (marks >= 96) return "A+";
            if (marks >= 90) return "A";
            if (marks >= 80) return "B+";
            if (marks >= 70) return "B";
            if (marks >= 50) return "C";
            if (marks >= 35) return "D";
            return "F";
        }
    }

    static class HistoryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                Integer userId = authenticate(exchange);
                if (userId == null) {
                    sendJsonResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                    return;
                }

                StringBuilder jsonBuilder = new StringBuilder("[");
                try (Connection conn = getConnection()) {
                    if (conn != null) {
                        try (PreparedStatement pstmt = conn.prepareStatement("SELECT roll_number, total_subjects, total_marks, average_percentage, grade, average_score, highest_score, lowest_score, subject_breakdown FROM grades WHERE user_id = ? ORDER BY id DESC")) {
                            pstmt.setInt(1, userId);
                            ResultSet rs = pstmt.executeQuery();
                            boolean first = true;
                            while (rs.next()) {
                                if (!first) jsonBuilder.append(",");
                                first = false;
                                jsonBuilder.append(String.format("{\"rollNumber\":\"%s\",\"totalSubjects\":%d,\"totalMarks\":%.2f,\"averagePercentage\":%.2f,\"grade\":\"%s\",\"averageScore\":%.2f,\"highestScore\":%.2f,\"lowestScore\":%.2f,\"subjectBreakdown\":\"%s\"}",
                                        rs.getString("roll_number"), rs.getInt("total_subjects"), rs.getDouble("total_marks"), rs.getDouble("average_percentage"),
                                        rs.getString("grade"), rs.getDouble("average_score"), rs.getDouble("highest_score"), rs.getDouble("lowest_score"),
                                        rs.getString("subject_breakdown").replace("\"", "\\\"")));
                            }
                        }
                    } else {
                        boolean first = true;
                        // Iterate in reverse to show latest first
                        for (int i = fallbackGrades.size() - 1; i >= 0; i--) {
                            FallbackGrade fg = fallbackGrades.get(i);
                            if (fg.userId == userId) {
                                if (!first) jsonBuilder.append(",");
                                first = false;
                                jsonBuilder.append(String.format("{\"rollNumber\":\"%s\",\"totalSubjects\":%d,\"totalMarks\":%.2f,\"averagePercentage\":%.2f,\"grade\":\"%s\",\"averageScore\":%.2f,\"highestScore\":%.2f,\"lowestScore\":%.2f,\"subjectBreakdown\":\"%s\"}",
                                        fg.rollNumber, fg.totalSubjects, fg.totalMarks, fg.averagePercentage,
                                        fg.grade, fg.averageScore, fg.highestScore, fg.lowestScore,
                                        fg.subjectBreakdown.replace("\"", "\\\"")));
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    sendJsonResponse(exchange, 500, "{\"error\":\"Server error\"}");
                    return;
                }
                jsonBuilder.append("]");
                sendJsonResponse(exchange, 200, jsonBuilder.toString());
            } else {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
            }
        }
    }
}
