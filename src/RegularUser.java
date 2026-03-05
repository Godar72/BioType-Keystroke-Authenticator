public class RegularUser extends User {
    public RegularUser(String username) {
        super(username);
    }

    @Override
    public void displayDashboard() {
        System.out.println("Opening User Panel: Authenticate & Enroll");
    }
}