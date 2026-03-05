public class Admin extends User {
    public Admin(String username) {
        super(username);
    }

    @Override
    public void displayDashboard() {
        System.out.println("Opening Admin Panel: View Logs & Manage Users");
    }
}