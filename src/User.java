public abstract class User {
    protected String username;

    public User(String username) {
        this.username = username;
    }

    // Abstract method forcing distinct behavior in child classes
    public abstract void displayDashboard();
}