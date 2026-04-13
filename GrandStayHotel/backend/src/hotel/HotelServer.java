package hotel;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import org.json.*;

public class HotelServer {

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // CORS + Routes
        server.createContext("/api/admin/login", new AdminLoginHandler());
        server.createContext("/api/customer/signup", new CustomerSignupHandler());
        server.createContext("/api/customer/login", new CustomerLoginHandler());
        server.createContext("/api/customers/pending", new PendingCustomersHandler());
        server.createContext("/api/customers/approve", new ApproveCustomerHandler());
        server.createContext("/api/rooms", new RoomsHandler());
        server.createContext("/api/rooms/add", new AddRoomHandler());
        server.createContext("/api/rooms/update", new UpdateRoomHandler());
        server.createContext("/api/rooms/delete", new DeleteRoomHandler());
        server.createContext("/api/checkin", new CheckInHandler());
        server.createContext("/api/checkout", new CheckOutHandler());
        server.createContext("/api/checkout/search", new CheckoutSearchHandler());
        server.createContext("/api/bookings", new BookingRequestsHandler());
        server.createContext("/api/bookings/add", new AddBookingHandler());
        server.createContext("/api/bookings/confirm", new ConfirmBookingHandler());
        server.createContext("/api/bills", new BillsHandler());
        server.createContext("/api/chat/messages", new ChatMessagesHandler());
        server.createContext("/api/chat/send", new SendChatHandler());
        server.createContext("/api/customers/all", new AllCustomersHandler());

        server.setExecutor(null);
        System.out.println("Grand Stay Hotel Server running on port 8080...");
        server.start();
    }

    // Utility: Read request body
    static String readBody(HttpExchange ex) throws IOException {
        InputStream is = ex.getRequestBody();
        return new String(is.readAllBytes());
    }

    // Utility: Send JSON response
    static void sendResponse(HttpExchange ex, int code, String json) throws IOException {
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        byte[] bytes = json.getBytes();
        ex.sendResponseHeaders(code, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.getResponseBody().close();
    }

    // Handle OPTIONS preflight
    static boolean handleOptions(HttpExchange ex) throws IOException {
        if (ex.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            ex.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }

    // ========================
    // ADMIN LOGIN (Fixed Credentials)
    // ========================
    static class AdminLoginHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            if (handleOptions(ex)) return;
            try {
                String body = readBody(ex);
                JSONObject req = new JSONObject(body);
                String email = req.getString("email");
                String password = req.getString("password");

                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM admin WHERE email=? AND password=?"
                );
                ps.setString(1, email);
                ps.setString(2, password);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    JSONObject resp = new JSONObject();
                    resp.put("success", true);
                    resp.put("message", "Admin login successful");
                    resp.put("name", rs.getString("name"));
                    resp.put("role", "admin");
                    sendResponse(ex, 200, resp.toString());
                } else {
                    sendResponse(ex, 401, "{\"success\":false,\"message\":\"Invalid admin credentials\"}");
                }
            } catch (Exception e) {
                sendResponse(ex, 500, "{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ========================
    // CUSTOMER SIGNUP
    // ========================
    static class CustomerSignupHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            if (handleOptions(ex)) return;
            try {
                String body = readBody(ex);
                JSONObject req = new JSONObject(body);
                String name = req.getString("name");
                String email = req.getString("email");
                String password = req.getString("password");
                String secQuestion = req.optString("security_question", "");
                String secAnswer = req.optString("security_answer", "");

                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO customers (name, email, password, security_question, security_answer, is_approved) VALUES (?,?,?,?,?,0)"
                );
                ps.setString(1, name);
                ps.setString(2, email);
                ps.setString(3, password);
                ps.setString(4, secQuestion);
                ps.setString(5, secAnswer);
                ps.executeUpdate();

                sendResponse(ex, 200, "{\"success\":true,\"message\":\"Signup successful! Wait for admin approval.\"}");
            } catch (SQLIntegrityConstraintViolationException e) {
                sendResponse(ex, 409, "{\"success\":false,\"message\":\"Email already registered.\"}");
            } catch (Exception e) {
                sendResponse(ex, 500, "{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ========================
    // CUSTOMER LOGIN (only if approved)
    // ========================
    static class CustomerLoginHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            if (handleOptions(ex)) return;
            try {
                String body = readBody(ex);
                JSONObject req = new JSONObject(body);
                String email = req.getString("email");
                String password = req.getString("password");

                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM customers WHERE email=? AND password=?"
                );
                ps.setString(1, email);
                ps.setString(2, password);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    int isApproved = rs.getInt("is_approved");
                    if (isApproved == 1) {
                        JSONObject resp = new JSONObject();
                        resp.put("success", true);
                        resp.put("message", "Login successful");
                        resp.put("name", rs.getString("name"));
                        resp.put("email", rs.getString("email"));
                        resp.put("role", "customer");
                        sendResponse(ex, 200, resp.toString());
                    } else if (isApproved == 0) {
                        sendResponse(ex, 403, "{\"success\":false,\"message\":\"Your account is pending admin approval.\"}");
                    } else {
                        sendResponse(ex, 403, "{\"success\":false,\"message\":\"Your account has been rejected.\"}");
                    }
                } else {
                    sendResponse(ex, 401, "{\"success\":false,\"message\":\"Invalid email or password.\"}");
                }
            } catch (Exception e) {
                sendResponse(ex, 500, "{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ========================
    // GET PENDING CUSTOMERS
    // ========================
    static class PendingCustomersHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            if (handleOptions(ex)) return;
            try {
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, name, email, mobile, created_at, is_approved FROM customers ORDER BY created_at DESC"
                );
                ResultSet rs = ps.executeQuery();
                JSONArray arr = new JSONArray();
                while (rs.next()) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", rs.getInt("id"));
                    obj.put("name", rs.getString("name"));
                    obj.put("email", rs.getString("email"));
                    obj.put("mobile", rs.getString("mobile") != null ? rs.getString("mobile") : "");
                    obj.put("created_at", rs.getString("created_at"));
                    obj.put("is_approved", rs.getInt("is_approved"));
                    arr.put(obj);
                }
                sendResponse(ex, 200, arr.toString());
            } catch (Exception e) {
                sendResponse(ex, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ========================
    // APPROVE/REJECT CUSTOMER
    // ========================
    static class ApproveCustomerHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            if (handleOptions(ex)) return;
            try {
                String body = readBody(ex);
                JSONObject req = new JSONObject(body);
                int customerId = req.getInt("id");
                int status = req.getInt("status"); // 1=approve, 2=reject

                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                    "UPDATE customers SET is_approved=? WHERE id=?"
                );
                ps.setInt(1, status);
                ps.setInt(2, customerId);
                ps.executeUpdate();

                String msg = status == 1 ? "Customer approved" : "Customer rejected";
                sendResponse(ex, 200, "{\"success\":true,\"message\":\"" + msg + "\"}");
            } catch (Exception e) {
                sendResponse(ex, 500, "{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ========================
    // GET ALL ROOMS
    // ========================
    static class RoomsHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            if (handleOptions(ex)) return;
            try {
                Connection conn = DatabaseConnection.getConnection();
                ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT * FROM rooms ORDER BY room_number"
                );
                JSONArray arr = new JSONArray();
                while (rs.next()) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", rs.getInt("id"));
                    obj.put("room_number", rs.getString("room_number"));
                    obj.put("room_type", rs.getString("room_type"));
                    obj.put("bed_type", rs.getString("bed_type"));
                    obj.put("price_per_day", rs.getDouble("price_per_day"));
                    obj.put("status", rs.getString("status"));
                    arr.put(obj);
                }
                sendResponse(ex, 200, arr.toString());
            } catch (Exception e) {
                sendResponse(ex, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ========================
    // ADD ROOM
    // ========================
    static class AddRoomHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            if (handleOptions(ex)) return;
            try {
                String body = readBody(ex);
                JSONObject req = new JSONObject(body);
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO rooms (room_number, room_type, bed_type, price_per_day) VALUES (?,?,?,?)"
                );
                ps.setString(1, req.getString("room_number"));
                ps.setString(2, req.getString("room_type"));
                ps.setString(3, req.getString("bed_type"));
                ps.setDouble(4, req.getDouble("price_per_day"));
                ps.executeUpdate();
                sendResponse(ex, 200, "{\"success\":true,\"message\":\"Room added successfully\"}");
            } catch (Exception e) {
                sendResponse(ex, 500, "{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ========================
    // UPDATE ROOM
    // ========================
    static class UpdateRoomHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            if (handleOptions(ex)) return;
            try {
                String body = readBody(ex);
                JSONObject req = new JSONObject(body);
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                    "UPDATE rooms SET room_type=?, bed_type=?, price_per_day=? WHERE room_number=?"
                );
                ps.setString(1, req.getString("room_type"));
                ps.setString(2, req.getString("bed_type"));
                ps.setDouble(3, req.getDouble("price_per_day"));
                ps.setString(4, req.getString("room_number"));
                ps.executeUpdate();
                sendResponse(ex, 200, "{\"success\":true,\"message\":\"Room updated successfully\"}");
            } catch (Exception e) {
                sendResponse(ex, 500, "{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ========================
    // DELETE ROOM
    // ========================
    static class DeleteRoomHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            if (handleOptions(ex)) return;
            try {
                String body = readBody(ex);
                JSONObject req = new JSONObject(body);
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement("DELETE FROM rooms WHERE room_number=?");
                ps.setString(1, req.getString("room_number"));
                ps.executeUpdate();
                sendResponse(ex, 200, "{\"success\":true,\"message\":\"Room deleted successfully\"}");
            } catch (Exception e) {
                sendResponse(ex, 500, "{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ========================
    // CHECK IN
    // ========================
    static class CheckInHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            if (handleOptions(ex)) return;
            try {
                String body = readBody(ex);
                JSONObject req = new JSONObject(body);
                Connection conn = DatabaseConnection.getConnection();

                // Get room id and price
                PreparedStatement roomPs = conn.prepareStatement("SELECT id, price_per_day FROM rooms WHERE room_number=? AND status='Available'");
                roomPs.setString(1, req.getString("room_number"));
                ResultSet roomRs = roomPs.executeQuery();
                if (!roomRs.next()) {
                    sendResponse(ex, 400, "{\"success\":false,\"message\":\"Room not available\"}");
                    return;
                }
                int roomId = roomRs.getInt("id");
                double price = roomRs.getDouble("price_per_day");

                PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO checkins (customer_name, mobile, email, gender, nationality, aadhar_number, address, room_id, checkin_date, price_per_day) VALUES (?,?,?,?,?,?,?,?,?,?)"
                );
                ps.setString(1, req.getString("name"));
                ps.setString(2, req.getString("mobile"));
                ps.setString(3, req.optString("email", ""));
                ps.setString(4, req.optString("gender", ""));
                ps.setString(5, req.optString("nationality", ""));
                ps.setString(6, req.optString("aadhar_number", ""));
                ps.setString(7, req.optString("address", ""));
                ps.setInt(8, roomId);
                ps.setString(9, req.getString("checkin_date"));
                ps.setDouble(10, price);
                ps.executeUpdate();

                // Mark room as booked
                PreparedStatement updateRoom = conn.prepareStatement("UPDATE rooms SET status='Booked' WHERE id=?");
                updateRoom.setInt(1, roomId);
                updateRoom.executeUpdate();

                sendResponse(ex, 200, "{\"success\":true,\"message\":\"Customer checked in successfully\"}");
            } catch (Exception e) {
                sendResponse(ex, 500, "{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ========================
    // SEARCH FOR CHECKOUT
    // ========================
    static class CheckoutSearchHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            if (handleOptions(ex)) return;
            try {
                String query = ex.getRequestURI().getQuery();
                String roomNumber = query.split("=")[1];
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                    "SELECT c.*, r.room_number, r.bed_type, r.room_type FROM checkins c JOIN rooms r ON c.room_id = r.id WHERE r.room_number=? AND c.status='Checked In'"
                );
                ps.setString(1, roomNumber);
                ResultSet rs = ps.executeQuery();
                JSONArray arr = new JSONArray();
                while (rs.next()) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", rs.getInt("id"));
                    obj.put("name", rs.getString("customer_name"));
                    obj.put("mobile", rs.getString("mobile"));
                    obj.put("email", rs.getString("email"));
                    obj.put("nationality", rs.getString("nationality"));
                    obj.put("gender", rs.getString("gender"));
                    obj.put("aadhar_number", rs.getString("aadhar_number"));
                    obj.put("address", rs.getString("address"));
                    obj.put("room_number", rs.getString("room_number"));
                    obj.put("bed_type", rs.getString("bed_type"));
                    obj.put("room_type", rs.getString("room_type"));
                    obj.put("checkin_date", rs.getString("checkin_date"));
                    obj.put("price_per_day", rs.getDouble("price_per_day"));
                    obj.put("room_id", rs.getInt("room_id"));
                    arr.put(obj);
                }
                sendResponse(ex, 200, arr.toString());
            } catch (Exception e) {
                sendResponse(ex, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ========================
    // CHECK OUT
    // ========================
    static class CheckOutHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            if (handleOptions(ex)) return;
            try {
                String body = readBody(ex);
                JSONObject req = new JSONObject(body);
                int checkinId = req.getInt("checkin_id");
                int roomId = req.getInt("room_id");
                String checkoutDate = req.getString("checkout_date");
                int numDays = req.getInt("num_days");
                double totalAmount = req.getDouble("total_amount");

                Connection conn = DatabaseConnection.getConnection();

                // Update checkin record
                PreparedStatement ps = conn.prepareStatement(
                    "UPDATE checkins SET checkout_date=?, total_amount=?, status='Checked Out' WHERE id=?"
                );
                ps.setString(1, checkoutDate);
                ps.setDouble(2, totalAmount);
                ps.setInt(3, checkinId);
                ps.executeUpdate();

                // Free the room
                PreparedStatement updateRoom = conn.prepareStatement("UPDATE rooms SET status='Available' WHERE id=?");
                updateRoom.setInt(1, roomId);
                updateRoom.executeUpdate();

                // Create bill
                PreparedStatement billPs = conn.prepareStatement(
                    "INSERT INTO bills (checkin_id, room_number, customer_name, mobile, nationality, gender, checkin_date, checkout_date, num_days, total_amount) " +
                    "SELECT ?, r.room_number, c.customer_name, c.mobile, c.nationality, c.gender, c.checkin_date, ?, ?, ? FROM checkins c JOIN rooms r ON c.room_id=r.id WHERE c.id=?"
                );
                billPs.setInt(1, checkinId);
                billPs.setString(2, checkoutDate);
                billPs.setInt(3, numDays);
                billPs.setDouble(4, totalAmount);
                billPs.setInt(5, checkinId);
                billPs.executeUpdate();

                sendResponse(ex, 200, "{\"success\":true,\"message\":\"Customer checked out successfully\"}");
            } catch (Exception e) {
                sendResponse(ex, 500, "{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ========================
    // GET BOOKING REQUESTS
    // ========================
    static class BookingRequestsHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            if (handleOptions(ex)) return;
            try {
                Connection conn = DatabaseConnection.getConnection();
                ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT * FROM booking_requests ORDER BY created_at DESC"
                );
                JSONArray arr = new JSONArray();
                while (rs.next()) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", rs.getInt("id"));
                    obj.put("customer_email", rs.getString("customer_email"));
                    obj.put("customer_name", rs.getString("customer_name") != null ? rs.getString("customer_name") : "");
                    obj.put("room_type", rs.getString("room_type") != null ? rs.getString("room_type") : "");
                    obj.put("checkin_date", rs.getString("checkin_date") != null ? rs.getString("checkin_date") : "");
                    obj.put("checkout_date", rs.getString("checkout_date") != null ? rs.getString("checkout_date") : "");
                    obj.put("num_guests", rs.getInt("num_guests"));
                    obj.put("total_cost", rs.getDouble("total_cost"));
                    obj.put("paid_amount", rs.getDouble("paid_amount"));
                    obj.put("status", rs.getString("status"));
                    arr.put(obj);
                }
                sendResponse(ex, 200, arr.toString());
            } catch (Exception e) {
                sendResponse(ex, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ========================
    // ADD BOOKING REQUEST (Customer)
    // ========================
    static class AddBookingHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            if (handleOptions(ex)) return;
            try {
                String body = readBody(ex);
                JSONObject req = new JSONObject(body);
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO booking_requests (customer_email, customer_name, room_type, checkin_date, checkout_date, num_guests, total_cost, paid_amount) VALUES (?,?,?,?,?,?,?,?)"
                );
                ps.setString(1, req.getString("customer_email"));
                ps.setString(2, req.optString("customer_name", ""));
                ps.setString(3, req.optString("room_type", ""));
                ps.setString(4, req.optString("checkin_date", null));
                ps.setString(5, req.optString("checkout_date", null));
                ps.setInt(6, req.optInt("num_guests", 1));
                ps.setDouble(7, req.optDouble("total_cost", 0));
                ps.setDouble(8, req.optDouble("paid_amount", 0));
                ps.executeUpdate();
                sendResponse(ex, 200, "{\"success\":true,\"message\":\"Booking request submitted!\"}");
            } catch (Exception e) {
                sendResponse(ex, 500, "{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ========================
    // CONFIRM BOOKING
    // ========================
    static class ConfirmBookingHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            if (handleOptions(ex)) return;
            try {
                String body = readBody(ex);
                JSONObject req = new JSONObject(body);
                int bookingId = req.getInt("booking_id");
                Connection conn = DatabaseConnection.getConnection();

                // Update booking status
                PreparedStatement ps = conn.prepareStatement("UPDATE booking_requests SET status='Confirmed' WHERE id=?");
                ps.setInt(1, bookingId);
                ps.executeUpdate();

                // Save confirmation details
                PreparedStatement confPs = conn.prepareStatement(
                    "INSERT INTO booking_confirmations (booking_id, customer_email, room_number, pending_payment, description) VALUES (?,?,?,?,?)"
                );
                confPs.setInt(1, bookingId);
                confPs.setString(2, req.optString("customer_email", ""));
                confPs.setString(3, req.optString("room_number", ""));
                confPs.setDouble(4, req.optDouble("pending_payment", 0));
                confPs.setString(5, req.optString("description", ""));
                confPs.executeUpdate();

                sendResponse(ex, 200, "{\"success\":true,\"message\":\"Booking confirmed!\"}");
            } catch (Exception e) {
                sendResponse(ex, 500, "{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ========================
    // GET BILLS
    // ========================
    static class BillsHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            if (handleOptions(ex)) return;
            try {
                String queryStr = ex.getRequestURI().getQuery();
                String dateFilter = "";
                if (queryStr != null && queryStr.contains("date=")) {
                    dateFilter = queryStr.split("date=")[1];
                }
                Connection conn = DatabaseConnection.getConnection();
                String sql = "SELECT * FROM bills" + (dateFilter.isEmpty() ? "" : " WHERE checkout_date='" + dateFilter + "'") + " ORDER BY created_at DESC";
                ResultSet rs = conn.createStatement().executeQuery(sql);
                JSONArray arr = new JSONArray();
                while (rs.next()) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", rs.getInt("id"));
                    obj.put("room_number", rs.getString("room_number"));
                    obj.put("customer_name", rs.getString("customer_name"));
                    obj.put("mobile", rs.getString("mobile"));
                    obj.put("nationality", rs.getString("nationality") != null ? rs.getString("nationality") : "");
                    obj.put("gender", rs.getString("gender") != null ? rs.getString("gender") : "");
                    obj.put("checkin_date", rs.getString("checkin_date"));
                    obj.put("checkout_date", rs.getString("checkout_date"));
                    obj.put("num_days", rs.getInt("num_days"));
                    obj.put("total_amount", rs.getDouble("total_amount"));
                    arr.put(obj);
                }
                sendResponse(ex, 200, arr.toString());
            } catch (Exception e) {
                sendResponse(ex, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ========================
    // CHAT MESSAGES
    // ========================
    static class ChatMessagesHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            if (handleOptions(ex)) return;
            try {
                Connection conn = DatabaseConnection.getConnection();
                ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM chat_messages ORDER BY sent_at DESC");
                JSONArray arr = new JSONArray();
                while (rs.next()) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", rs.getInt("id"));
                    obj.put("guest_email", rs.getString("guest_email"));
                    obj.put("message", rs.getString("message"));
                    obj.put("sender", rs.getString("sender"));
                    obj.put("sent_at", rs.getString("sent_at"));
                    arr.put(obj);
                }
                sendResponse(ex, 200, arr.toString());
            } catch (Exception e) {
                sendResponse(ex, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ========================
    // SEND CHAT MESSAGE
    // ========================
    static class SendChatHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            if (handleOptions(ex)) return;
            try {
                String body = readBody(ex);
                JSONObject req = new JSONObject(body);
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO chat_messages (guest_email, message, sender) VALUES (?,?,?)"
                );
                ps.setString(1, req.getString("guest_email"));
                ps.setString(2, req.getString("message"));
                ps.setString(3, req.optString("sender", "Guest"));
                ps.executeUpdate();
                sendResponse(ex, 200, "{\"success\":true,\"message\":\"Message sent\"}");
            } catch (Exception e) {
                sendResponse(ex, 500, "{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ========================
    // ALL CUSTOMERS (for admin manage users)
    // ========================
    static class AllCustomersHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            if (handleOptions(ex)) return;
            try {
                Connection conn = DatabaseConnection.getConnection();
                ResultSet rs = conn.createStatement().executeQuery("SELECT id, name, email, mobile, is_approved, created_at FROM customers ORDER BY created_at DESC");
                JSONArray arr = new JSONArray();
                while (rs.next()) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", rs.getInt("id"));
                    obj.put("name", rs.getString("name"));
                    obj.put("email", rs.getString("email"));
                    obj.put("mobile", rs.getString("mobile") != null ? rs.getString("mobile") : "");
                    obj.put("is_approved", rs.getInt("is_approved"));
                    obj.put("created_at", rs.getString("created_at"));
                    arr.put(obj);
                }
                sendResponse(ex, 200, arr.toString());
            } catch (Exception e) {
                sendResponse(ex, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }
}
