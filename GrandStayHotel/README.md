# 🏨 Grand Stay Hotel Management System
## HTML Frontend + Java Backend + MySQL

---

## 🗂 Project Structure

```
HotelManagement/
├── database/
│   └── schema.sql              ← Run this in MySQL Workbench first
├── backend/
│   └── src/hotel/
│       ├── HotelServer.java    ← Main API server (port 8080)
│       └── DatabaseConnection.java
└── frontend/
    ├── index.html              ← Login page (Admin + Customer tabs)
    ├── signup.html             ← Customer signup
    ├── forgot.html             ← Forgot password
    ├── admin.html              ← Admin dashboard
    └── customer.html           ← Customer portal
```

---

## ⚙️ SETUP INSTRUCTIONS

### Step 1: MySQL Workbench Setup
1. Open MySQL Workbench
2. Open `database/schema.sql`
3. Run the entire script (Ctrl+Shift+Enter)
4. This creates the `grand_stay_hotel` database with all tables

### Step 2: Configure Database Connection
Edit `backend/src/hotel/DatabaseConnection.java`:
```java
private static final String URL = "jdbc:mysql://localhost:3306/grand_stay_hotel";
private static final String USER = "root";           // ← Your MySQL username
private static final String PASSWORD = "your_pass";  // ← Your MySQL password
```

### Step 3: Download Required JARs
Download these JARs and put them in `backend/lib/`:
- `mysql-connector-j-8.x.x.jar` → https://dev.mysql.com/downloads/connector/j/
- `json-20231013.jar` → https://mvnrepository.com/artifact/org.json/json

### Step 4: Compile the Java Backend
```bash
# Create lib and out directories
mkdir -p backend/lib backend/out

# Compile (Windows)
javac -cp "backend/lib/*" -d backend/out backend/src/hotel/*.java

# Compile (Mac/Linux)
javac -cp "backend/lib/*" -d backend/out backend/src/hotel/*.java
```

### Step 5: Run the Backend Server
```bash
# Windows
java -cp "backend/out;backend/lib/*" hotel.HotelServer

# Mac/Linux
java -cp "backend/out:backend/lib/*" hotel.HotelServer
```
You should see: `Grand Stay Hotel Server running on port 8080...`

### Step 6: Open the Frontend
Simply open `frontend/index.html` in your browser.
(Or use VS Code Live Server extension)

---

## 🔐 LOGIN CREDENTIALS

### Admin Login (FIXED — cannot be changed)
- **Email:** `admin@grandstayhotel.com`
- **Password:** `Admin@123`

### Customer Login
- Customers must SIGN UP first (from login page → Sign Up Now)
- After signup, admin must APPROVE the account
- Only approved customers can log in

---

## 🔄 WORKFLOW

### Customer Flow:
1. Open `index.html`
2. Click "Don't have an account? Sign Up"
3. Fill in details and sign up
4. Wait for admin approval (status: Pending)
5. Admin approves → Customer can log in
6. Customer portal: View hotel photos, contact info, chat, book rooms

### Admin Flow:
1. Open `index.html` → Click "Admin" tab
2. Login with fixed admin credentials
3. Dashboard shows all stats
4. **Manage Users** → Approve/Reject customer signups
5. **Manage Rooms** → Add/Update/Delete rooms
6. **Customer Check In** → Check in walk-in guests
7. **Customer Check Out** → Check out guests + auto-generate bill
8. **Booking Requests** → View online booking requests
9. **Booking Confirmation** → Confirm bookings with room assignment
10. **Customer Bills** → Search bills by checkout date
11. **Reply to Chats** → Reply to customer messages

---

## 📡 API ENDPOINTS

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/admin/login` | Admin login (fixed credentials) |
| POST | `/api/customer/signup` | Customer registration |
| POST | `/api/customer/login` | Customer login (if approved) |
| GET | `/api/customers/pending` | All customers list |
| GET | `/api/customers/all` | All customers |
| POST | `/api/customers/approve` | Approve/Reject customer |
| GET | `/api/rooms` | Get all rooms |
| POST | `/api/rooms/add` | Add room |
| POST | `/api/rooms/update` | Update room |
| POST | `/api/rooms/delete` | Delete room |
| POST | `/api/checkin` | Check in customer |
| GET | `/api/checkout/search?room=X` | Search for checkout |
| POST | `/api/checkout` | Check out customer |
| GET | `/api/bookings` | Get booking requests |
| POST | `/api/bookings/add` | Submit booking request |
| POST | `/api/bookings/confirm` | Confirm booking |
| GET | `/api/bills` | Get bills |
| GET | `/api/chat/messages` | Get chat messages |
| POST | `/api/chat/send` | Send chat message |

---

## 🛠 TROUBLESHOOTING

**"Cannot connect to server"**
→ Make sure `java HotelServer` is running on port 8080

**"MySQL JDBC Driver not found"**
→ Make sure `mysql-connector-j-*.jar` is in `backend/lib/`

**CORS errors in browser**
→ The server has CORS headers. Use a direct browser file or Live Server.

**Compilation error "cannot find symbol JSONObject"**
→ Make sure `json-20231013.jar` is in `backend/lib/`

---

## 📦 Dependencies
- Java 11+ (JDK)
- MySQL 8.0+
- MySQL Workbench
- mysql-connector-j JAR
- org.json JAR
