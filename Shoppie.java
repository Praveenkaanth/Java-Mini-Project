import javax.swing.*;
import javax.swing.border.EmptyBorder;
import com.mongodb.client.*;
import org.bson.Document;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;
import java.net.URL;
import javax.imageio.ImageIO;

public class OnlineGarmentShoppingApp {
    static final String MONGO_URI = "mongodb://localhost:27017";
    static final String DATABASE_NAME = "garment_shop";
    static final String USERS_COLLECTION = "users";
    static final String GARMENTS_COLLECTION = "garments";
    static final String CART_COLLECTION = "shopping_cart";
    static final String ORDERS_COLLECTION = "orders";

    private JFrame frame;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private MongoClient mongoClient;
    private MongoDatabase database;
    private String currentUser;
    private static final Dimension STANDARD_SIZE = new Dimension(1200, 800);
    private static final Dimension ITEM_SIZE = new Dimension(300, 200);
    private static final Dimension SIDEBAR_SIZE = new Dimension(250, STANDARD_SIZE.height);

    // Color schemes for monochromatic design
    private static class ColorScheme {
        Color primaryColor;
        Color secondaryColor;
        Color accentColor;
        Color textColor;
        Color backgroundColor;

        ColorScheme(Color primary, Color secondary, Color accent, Color text, Color background) {
            this.primaryColor = primary;
            this.secondaryColor = secondary;
            this.accentColor = accent;
            this.textColor = text;
            this.backgroundColor = background;
        }
    }

    // Monochromatic light scheme
    private static final ColorScheme LIGHT_SCHEME = new ColorScheme(
        new Color(50, 50, 50),    // Primary
        new Color(240, 240, 240), // Secondary
        new Color(100, 100, 100), // Accent
        new Color(30, 30, 30),    // Text
        new Color(255, 255, 255)  // Background
    );

    // Monochromatic dark scheme
    private static final ColorScheme DARK_SCHEME = new ColorScheme(
        new Color(200, 200, 200), // Primary
        new Color(40, 40, 40),    // Secondary
        new Color(150, 150, 150), // Accent
        new Color(220, 220, 220), // Text
        new Color(20, 20, 20)     // Background
    );

    private ColorScheme currentScheme = LIGHT_SCHEME;

    // Font constants
    private static final Font TITLE_FONT = new Font("Helvetica", Font.BOLD, 24);
    private static final Font HEADER_FONT = new Font("Helvetica", Font.BOLD, 18);
    private static final Font BODY_FONT = new Font("Helvetica", Font.PLAIN, 14);
    private static final Font BUTTON_FONT = new Font("Helvetica", Font.BOLD, 14);

    private JPanel sidebar;
    private Timer sidebarTimer;
    private boolean isSidebarVisible = false;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new OnlineGarmentShoppingApp().displayLogin();
        });
    }

    public OnlineGarmentShoppingApp() {
        mongoClient = MongoClients.create(MONGO_URI);
        database = mongoClient.getDatabase(DATABASE_NAME);
        addSampleGarments();
    }

    private void addSampleGarments() {
        MongoCollection<Document> garmentsCollection = database.getCollection(GARMENTS_COLLECTION);
        if (garmentsCollection.countDocuments() == 0) {
            List<Document> sampleGarments = new ArrayList<>();
            sampleGarments.add(createGarment("Modern T-Shirt", 29.99, "Clothing", 
                "https://example.com/modern-tshirt.jpg", List.of("S", "M", "L", "XL")));
            sampleGarments.add(createGarment("Designer Jeans", 79.99, "Clothing",
                "https://example.com/designer-jeans.jpg", List.of("28", "30", "32", "34", "36")));
            sampleGarments.add(createGarment("Sleek Jacket", 129.99, "Clothing",
                "https://example.com/sleek-jacket.jpg", List.of("S", "M", "L", "XL")));
            sampleGarments.add(createGarment("Trendy Sneakers", 89.99, "Footwear",
                "https://example.com/trendy-sneakers.jpg", List.of("7", "8", "9", "10", "11")));
            sampleGarments.add(createGarment("Stylish Hat", 34.99, "Accessories",
                "https://example.com/stylish-hat.jpg", List.of("S", "M", "L")));
            
            garmentsCollection.insertMany(sampleGarments);
            System.out.println("Sample garments added to the database.");
        }
    }

    private Document createGarment(String name, double price, String category, String image, List<String> sizes) {
        return new Document()
            .append("name", name)
            .append("price", price)
            .append("category", category)
            .append("image", image)
            .append("sizes", sizes);
    }

    private JButton createStyledButton(String text, ActionListener actionListener) {
        JButton button = new JButton(text);
        button.setFont(BUTTON_FONT);
        button.setForeground(Color.WHITE);
        button.setBackground(currentScheme.primaryColor);
        button.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        button.setFocusPainted(false);
        button.addActionListener(actionListener);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(currentScheme.primaryColor.darker());
            }
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(currentScheme.primaryColor);
            }
        });
        return button;
    }

    private JTextField createStyledTextField(String placeholder) {
        JTextField textField = new JTextField(20);
        textField.setFont(BODY_FONT);
        textField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(currentScheme.primaryColor),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        textField.setForeground(currentScheme.textColor);
        textField.setText(placeholder);
        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (textField.getText().equals(placeholder)) {
                    textField.setText("");
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (textField.getText().isEmpty()) {
                    textField.setText(placeholder);
                }
            }
        });
        return textField;
    }

    private JPasswordField createStyledPasswordField(String placeholder) {
        JPasswordField passwordField = new JPasswordField(20);
        passwordField.setFont(BODY_FONT);
        passwordField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(currentScheme.primaryColor),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        passwordField.setForeground(currentScheme.textColor);
        passwordField.setEchoChar((char) 0);
        passwordField.setText(placeholder);
        passwordField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (String.valueOf(passwordField.getPassword()).equals(placeholder)) {
                    passwordField.setText("");
                    passwordField.setEchoChar('•');
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (passwordField.getPassword().length == 0) {
                    passwordField.setText(placeholder);
                    passwordField.setEchoChar((char) 0);
                }
            }
        });
        return passwordField;
    }

    public void displayLogin() {
        frame = new JFrame("Shoppie - Login");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(STANDARD_SIZE);
        frame.setLayout(new BorderLayout());
        updateUIColors();

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(currentScheme.backgroundColor);
        panel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 0, 10, 0);

        JLabel titleLabel = new JLabel("Shoppie", SwingConstants.CENTER);
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(currentScheme.primaryColor);
        panel.add(titleLabel, gbc);

        usernameField = createStyledTextField("Username");
        passwordField = createStyledPasswordField("Password");

        panel.add(usernameField, gbc);
        panel.add(passwordField, gbc);

        panel.add(createStyledButton("Login", e -> login()), gbc);
        panel.add(createStyledButton("Register", e -> register()), gbc);

        frame.add(panel, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    public void login() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        MongoCollection<Document> usersCollection = database.getCollection(USERS_COLLECTION);
        Document query = new Document("username", username).append("password", password);
        Document user = usersCollection.find(query).first();

        if (user != null) {
            currentUser = username;
            JOptionPane.showMessageDialog(frame, "Login successful!");
            viewProducts();
        } else {
            JOptionPane.showMessageDialog(frame, "Invalid username or password.");
        }
    }

    public void register() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        MongoCollection<Document> usersCollection = database.getCollection(USERS_COLLECTION);
        Document query = new Document("username", username);
        Document existingUser = usersCollection.find(query).first();

        if (existingUser != null) {
            JOptionPane.showMessageDialog(frame, "Username already exists. Please choose another.");
        } else {
            Document newUser = new Document("username", username).append("password", password);
            usersCollection.insertOne(newUser);
            JOptionPane.showMessageDialog(frame, "Registration successful! You can now log in.");
        }
    }

    private void updateUIColors() {
        frame.getContentPane().setBackground(currentScheme.backgroundColor);
        updateComponentColors(frame.getContentPane());
    }

    private void updateComponentColors(Container container) {
        for (Component c : container.getComponents()) {
            if (c instanceof JPanel) {
                c.setBackground(currentScheme.backgroundColor);
            } else if (c instanceof JButton) {
                JButton button = (JButton) c;
                button.setBackground(currentScheme.primaryColor);
                button.setForeground(Color.WHITE);
            } else if (c instanceof JLabel) {
                JLabel label = (JLabel) c;
                label.setForeground(currentScheme.textColor);
            }
            if (c instanceof Container) {
                updateComponentColors((Container) c);
            }
        }
    }

    private ImageIcon loadImage(String path, int width, int height) {
        if (path == null || path.isEmpty()) {
            System.out.println("Warning: Image path is null or empty");
            return createPlaceholderImage(width, height);
        }
        
        try {
            URL imageUrl = new URL(path);
            Image image = ImageIO.read(imageUrl);
            if (image != null) {
                Image scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                return new ImageIcon(scaledImage);
            } else {
                System.out.println("Warning: Could not read image from URL: " + path);
                return createPlaceholderImage(width, height);
            }
        } catch (Exception e) {
            System.out.println("Error loading image from URL: " + path);
            e.printStackTrace();
            return createPlaceholderImage(width, height);
        }
    }

    private ImageIcon createPlaceholderImage(int width, int height) {
        BufferedImage placeholderImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = placeholderImage.createGraphics();
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillRect(0, 0, width, height);
        g2d.setColor(Color.GRAY);
        g2d.drawRect(0, 0, width - 1, height - 1);
        g2d.setColor(Color.DARK_GRAY);
        g2d.drawString("No Image", 10, height / 2);
        g2d.dispose();
        return new ImageIcon(placeholderImage);
    }

    private String getSelectedSize(ButtonGroup sizeGroup) {
        for (Enumeration<AbstractButton> buttons = sizeGroup.getElements(); buttons.hasMoreElements();) {
            AbstractButton button = buttons.nextElement();
            if (button.isSelected()) {
                return button.getText();
            }
        }
        return null;
    }

    private JPanel createProductCard(Document garment) {
        JPanel cardPanel = new JPanel(new BorderLayout(10, 10));
        cardPanel.setBackground(currentScheme.secondaryColor);
        cardPanel.setBorder(BorderFactory.createLineBorder(currentScheme.primaryColor, 1));
        cardPanel.setPreferredSize(ITEM_SIZE);

        String imagePath = garment.getString("image");
        ImageIcon garmentImage = loadImage(imagePath, 100, 100);
        JLabel imageLabel = new JLabel(garmentImage);
        imageLabel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel infoPanel = new JPanel(new GridLayout(6, 1, 5, 5));
        infoPanel.setBackground(currentScheme.secondaryColor);
        infoPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel nameLabel = new JLabel(garment.getString("name"));
        nameLabel.setFont(HEADER_FONT);
        nameLabel.setForeground(currentScheme.textColor);

        JLabel priceLabel = new JLabel("$" + String.format("%.2f", garment.getDouble("price")));
        priceLabel.setFont(BODY_FONT);
        priceLabel.setForeground(currentScheme.accentColor);

        JLabel sizeLabel = new JLabel("Size:");
        sizeLabel.setFont(BODY_FONT);
        sizeLabel.setForeground(currentScheme.textColor);

        JPanel sizePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        sizePanel.setBackground(currentScheme.secondaryColor);
        ButtonGroup sizeGroup = new ButtonGroup();
        String[] sizes = {"S", "M", "L", "XL"};
        for (String size : sizes) {
            JRadioButton sizeButton = new JRadioButton(size);
            sizeButton.setFont(BODY_FONT);
            sizeButton.setBackground(currentScheme.secondaryColor);
            sizeButton.setForeground(currentScheme.textColor);
            sizeGroup.add(sizeButton);
            sizePanel.add(sizeButton);
        }

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        buttonPanel.setBackground(currentScheme.secondaryColor);

        JButton addToCartButton = createStyledButton("Add to Cart", e -> addToCart(garment, getSelectedSize(sizeGroup)));
        JButton buyNowButton = createStyledButton("Buy Now", e -> buyNow(garment, getSelectedSize(sizeGroup)));

        buttonPanel.add(addToCartButton);
        buttonPanel.add(buyNowButton);

        infoPanel.add(nameLabel);
        infoPanel.add(priceLabel);
        infoPanel.add(sizeLabel);
        infoPanel.add(sizePanel);
        infoPanel.add(Box.createVerticalStrut(10));
        infoPanel.add(buttonPanel);

        cardPanel.add(imageLabel, BorderLayout.WEST);
        cardPanel.add(infoPanel, BorderLayout.CENTER);

        return cardPanel;
    }

    private JPanel createCartItemPanel(Document cartItem, Document garment) {
        JPanel itemPanel = new JPanel(new BorderLayout(10, 10));
        itemPanel.setBackground(currentScheme.secondaryColor);
        itemPanel.setBorder(BorderFactory.createLineBorder(currentScheme.primaryColor));
        itemPanel.setPreferredSize(ITEM_SIZE);

        String imagePath = garment.getString("image");
        ImageIcon garmentImage = loadImage(imagePath, 100, 100);
        JLabel imageLabel = new JLabel(garmentImage);
        imageLabel.setBorder(new EmptyBorder(5, 5, 5, 5));

        JPanel infoPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        infoPanel.setBackground(currentScheme.secondaryColor);

        JLabel nameLabel = new JLabel(garment.getString("name"));
        nameLabel.setFont(HEADER_FONT);
        nameLabel.setForeground(currentScheme.textColor);

        JLabel priceLabel = new JLabel("$" + String.format("%.2f", garment.getDouble("price")));
        priceLabel.setFont(BODY_FONT);
        priceLabel.setForeground(currentScheme.accentColor);

        JLabel sizeLabel = new JLabel("Size: " + cartItem.getString("size"));
        sizeLabel.setFont(BODY_FONT);
        sizeLabel.setForeground(currentScheme.textColor);

        infoPanel.add(nameLabel);
        infoPanel.add(priceLabel);
        infoPanel.add(sizeLabel);

        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        buttonPanel.setBackground(currentScheme.secondaryColor);

        JButton buyNowButton = createStyledButton("Buy Now", e -> {
            removeFromCart(cartItem);
            buyNow(garment, cartItem.getString("size"));
        });
        JButton removeButton = createStyledButton("Remove", e -> {
            removeFromCart(cartItem);
            itemPanel.getParent().remove(itemPanel);
            itemPanel.getParent().revalidate();
            itemPanel.getParent().repaint();
        });

        buttonPanel.add(buyNowButton);
        buttonPanel.add(removeButton);

        itemPanel.add(imageLabel, BorderLayout.WEST);
        itemPanel.add(infoPanel, BorderLayout.CENTER);
        itemPanel.add(buttonPanel, BorderLayout.EAST);

        return itemPanel;
    }

    public void addToCart(Document garment, String size) {
        if (size == null || size.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please select a size before adding to cart.");
            return;
        }
        MongoCollection<Document> cartCollection = database.getCollection(CART_COLLECTION);
        Document cartItem = new Document("username", currentUser)
                                .append("garment", garment)
                                .append("size", size);
        cartCollection.insertOne(cartItem);
        
        int option = JOptionPane.showConfirmDialog(frame, 
            "Item added to cart. Would you like to buy it now?", 
            "Added to Cart", 
            JOptionPane.YES_NO_OPTION);
            
        if (option == JOptionPane.YES_OPTION) {
            buyNow(garment, size);
        }
    }

    public void buyNow(Document garment, String size) {
        if (size == null || size.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please select a size before buying.");
            return;
        }
        JPanel panel = new JPanel(new GridLayout(0, 1));
        JTextField nameField = new JTextField(20);
        JTextField addressField = new JTextField(20);
        JTextField phoneField = new JTextField(20);

        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Address:"));
        panel.add(addressField);
        panel.add(new JLabel("Phone:"));
        panel.add(phoneField);

        int result = JOptionPane.showConfirmDialog(null, panel, "Enter Shipping Details",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText();
            String address = addressField.getText();
            String phone = phoneField.getText();

            MongoCollection<Document> ordersCollection = database.getCollection(ORDERS_COLLECTION);
            Document order = new Document("username", currentUser)
                                .append("garment", garment)
                                .append("size", size)
                                .append("name", name)
                                .append("address", address)
                                .append("phone", phone)
                                .append("status", "Placed");
            ordersCollection.insertOne(order);
            JOptionPane.showMessageDialog(frame, "Order placed successfully!");
        }
    }

    public void removeFromCart(Document cartItem) {
        MongoCollection<Document> cartCollection = database.getCollection(CART_COLLECTION);
        cartCollection.deleteOne(cartItem);
        JOptionPane.showMessageDialog(frame, "Item removed from cart.");
    }

    public void viewProducts() {
        frame.getContentPane().removeAll();
        frame.setLayout(new BorderLayout());
        updateUIColors();

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(currentScheme.primaryColor);
        headerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JButton menuButton = createStyledButton("☰", e -> toggleSidebar());
        menuButton.setPreferredSize(new Dimension(50, 50));
        headerPanel.add(menuButton, BorderLayout.WEST);

        JLabel titleLabel = new JLabel("Shoppie", SwingConstants.CENTER);
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        JButton cartButton = createStyledButton("🛒", e -> viewCart());
        cartButton.setPreferredSize(new Dimension(50, 50));
        headerPanel.add(cartButton, BorderLayout.EAST);

        frame.add(headerPanel, BorderLayout.NORTH);

        MongoCollection<Document> garmentsCollection = database.getCollection(GARMENTS_COLLECTION);
        List<Document> garments = garmentsCollection.find().into(new ArrayList<>());

        JPanel panel = new JPanel(new GridLayout(0, 3, 20, 20));
        panel.setBackground(currentScheme.backgroundColor);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        for (Document garment : garments) {
            JPanel cardPanel = createProductCard(garment);
            panel.add(cardPanel);
        }

        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(null);

        frame.add(scrollPane, BorderLayout.CENTER);

        createSidebar();

        frame.revalidate();
        frame.repaint();
    }

    private void createSidebar() {
        sidebar = new JPanel();
        sidebar.setPreferredSize(SIDEBAR_SIZE);
        sidebar.setBackground(currentScheme.secondaryColor);
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel userLabel = new JLabel("Welcome, " + currentUser);
        userLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        userLabel.setFont(HEADER_FONT);
        userLabel.setForeground(currentScheme.textColor);
        sidebar.add(userLabel);

        sidebar.add(Box.createRigidArea(new Dimension(0, 20)));

        JButton myOrdersButton = createStyledButton("My Orders", e -> viewOrders());
        myOrdersButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(myOrdersButton);

        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));

        JButton toggleThemeButton = createStyledButton("Toggle Theme", e -> toggleTheme());
        toggleThemeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(toggleThemeButton);

        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));

        JButton logoutButton = createStyledButton("Logout", e -> logout());
        logoutButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(logoutButton);

        sidebar.setVisible(false);
        frame.add(sidebar, BorderLayout.WEST);
    }

    private void toggleSidebar() {
        if (sidebarTimer != null && sidebarTimer.isRunning()) {
            return;
        }

        int start = isSidebarVisible ? SIDEBAR_SIZE.width : 0;
        int end = isSidebarVisible ? 0 : SIDEBAR_SIZE.width;
        int steps = 50;
        int delay = 5;

        sidebarTimer = new Timer(delay, null);
        sidebarTimer.addActionListener(new ActionListener() {
            int width = start;
            int step = (end - start) / steps;

            @Override
            public void actionPerformed(ActionEvent e) {
                width += step;
                sidebar.setPreferredSize(new Dimension(width, SIDEBAR_SIZE.height));
                sidebar.revalidate();

                if ((step > 0 && width >= end) || (step < 0 && width <= end)) {
                    sidebarTimer.stop();
                    isSidebarVisible = !isSidebarVisible;
                    sidebar.setVisible(isSidebarVisible);
                }
            }
        });

        sidebar.setVisible(true);
        sidebarTimer.start();
    }

    private void toggleTheme() {
        currentScheme = (currentScheme == LIGHT_SCHEME) ? DARK_SCHEME : LIGHT_SCHEME;
        updateUIColors();
        SwingUtilities.updateComponentTreeUI(frame);
        frame.repaint();
    }

    public void viewCart() {
        frame.getContentPane().removeAll();
        frame.setLayout(new BorderLayout());
        updateUIColors();

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(currentScheme.primaryColor);
        headerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JButton backButton = createStyledButton("Back", e -> viewProducts());
        headerPanel.add(backButton, BorderLayout.WEST);

        JLabel titleLabel = new JLabel("Your Cart", SwingConstants.CENTER);
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        frame.add(headerPanel, BorderLayout.NORTH);

        MongoCollection<Document> cartCollection = database.getCollection(CART_COLLECTION);
        List<Document> cartItems = cartCollection.find(new Document("username", currentUser)).into(new ArrayList<>());

        if (cartItems.isEmpty()) {
            JLabel emptyCartLabel = new JLabel("Your cart is empty.", SwingConstants.CENTER);
            emptyCartLabel.setFont(HEADER_FONT);
            frame.add(emptyCartLabel, BorderLayout.CENTER);
        } else {
            JPanel panel = new JPanel(new GridLayout(0, 1, 10, 10));
            panel.setBackground(currentScheme.backgroundColor);
            panel.setBorder(new EmptyBorder(20, 20, 20, 20));

            double total = 0;

            for (Document cartItem : cartItems) {
                Document garment = (Document) cartItem.get("garment");
                JPanel itemPanel = createCartItemPanel(cartItem, garment);
                panel.add(itemPanel);
                total += garment.getDouble("price");
            }

            JScrollPane scrollPane = new JScrollPane(panel);
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            scrollPane.getVerticalScrollBar().setUnitIncrement(16);
            scrollPane.setBorder(null);

            frame.add(scrollPane, BorderLayout.CENTER);

            JPanel totalPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            totalPanel.setBackground(currentScheme.secondaryColor);
            JLabel totalLabel = new JLabel("Total: $" + String.format("%.2f", total));
            totalLabel.setFont(HEADER_FONT);
            totalLabel.setForeground(currentScheme.primaryColor);
            totalPanel.add(totalLabel);

            JButton checkoutButton = createStyledButton("Checkout", e -> checkout(cartItems));
            totalPanel.add(checkoutButton);

            frame.add(totalPanel, BorderLayout.SOUTH);
        }

        frame.revalidate();
        frame.repaint();
    }

    public void checkout(List<Document> cartItems) {
        JPanel panel = new JPanel(new GridLayout(0, 1));
        JTextField nameField = new JTextField(20);
        JTextField addressField = new JTextField(20);
        JTextField phoneField = new JTextField(20);

        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Address:"));
        panel.add(addressField);
        panel.add(new JLabel("Phone:"));
        panel.add(phoneField);

         int result = JOptionPane.showConfirmDialog(null, panel, "Enter Shipping Details",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText();
            String address = addressField.getText();
            String phone = phoneField.getText();

            MongoCollection<Document> ordersCollection = database.getCollection(ORDERS_COLLECTION);
            MongoCollection<Document> cartCollection = database.getCollection(CART_COLLECTION);

            for (Document cartItem : cartItems) {
                Document order = new Document("username", currentUser)
                                    .append("garment", cartItem.get("garment"))
                                    .append("size", cartItem.getString("size"))
                                    .append("name", name)
                                    .append("address", address)
                                    .append("phone", phone)
                                    .append("status", "Placed");
                ordersCollection.insertOne(order);
                cartCollection.deleteOne(cartItem);
            }

            JOptionPane.showMessageDialog(frame, "Order placed successfully!");
            viewProducts();
        }
    }

    public void viewOrders() {
        frame.getContentPane().removeAll();
        frame.setLayout(new BorderLayout());
        updateUIColors();

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(currentScheme.primaryColor);
        headerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JButton backButton = createStyledButton("Back", e -> viewProducts());
        headerPanel.add(backButton, BorderLayout.WEST);

        JLabel titleLabel = new JLabel("Your Orders", SwingConstants.CENTER);
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        frame.add(headerPanel, BorderLayout.NORTH);

        MongoCollection<Document> ordersCollection = database.getCollection(ORDERS_COLLECTION);
        List<Document> orders = ordersCollection.find(new Document("username", currentUser)).into(new ArrayList<>());

        if (orders.isEmpty()) {
            JLabel emptyOrdersLabel = new JLabel("You have no orders.", SwingConstants.CENTER);
            emptyOrdersLabel.setFont(HEADER_FONT);
            frame.add(emptyOrdersLabel, BorderLayout.CENTER);
        } else {
            JPanel panel = new JPanel(new GridLayout(0, 1, 10, 10));
            panel.setBackground(currentScheme.backgroundColor);
            panel.setBorder(new EmptyBorder(20, 20, 20, 20));

            for (Document order : orders) {
                JPanel orderPanel = createOrderPanel(order);
                panel.add(orderPanel);
            }

            JScrollPane scrollPane = new JScrollPane(panel);
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            scrollPane.getVerticalScrollBar().setUnitIncrement(16);
            scrollPane.setBorder(null);

            frame.add(scrollPane, BorderLayout.CENTER);
        }

        frame.revalidate();
        frame.repaint();
    }

    private JPanel createOrderPanel(Document order) {
        JPanel orderPanel = new JPanel(new BorderLayout(10, 10));
        orderPanel.setBackground(currentScheme.secondaryColor);
        orderPanel.setBorder(BorderFactory.createLineBorder(currentScheme.primaryColor));
        orderPanel.setPreferredSize(ITEM_SIZE);

        Document garment = (Document) order.get("garment");
        String imagePath = garment.getString("image");
        ImageIcon garmentImage = loadImage(imagePath, 100, 100);
        JLabel imageLabel = new JLabel(garmentImage);
        imageLabel.setBorder(new EmptyBorder(5, 5, 5, 5));

        JPanel infoPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        infoPanel.setBackground(currentScheme.secondaryColor);

        JLabel nameLabel = new JLabel(garment.getString("name"));
        nameLabel.setFont(HEADER_FONT);
        nameLabel.setForeground(currentScheme.textColor);

        JLabel priceLabel = new JLabel("$" + String.format("%.2f", garment.getDouble("price")));
        priceLabel.setFont(BODY_FONT);
        priceLabel.setForeground(currentScheme.accentColor);

        JLabel sizeLabel = new JLabel("Size: " + order.getString("size"));
        sizeLabel.setFont(BODY_FONT);
        sizeLabel.setForeground(currentScheme.textColor);

        JLabel statusLabel = new JLabel("Status: " + order.getString("status"));
        statusLabel.setFont(BODY_FONT);
        statusLabel.setForeground(currentScheme.textColor);

        infoPanel.add(nameLabel);
        infoPanel.add(priceLabel);
        infoPanel.add(sizeLabel);
        infoPanel.add(statusLabel);

        orderPanel.add(imageLabel, BorderLayout.WEST);
        orderPanel.add(infoPanel, BorderLayout.CENTER);

        return orderPanel;
    }

    public void logout() {
        currentUser = null;
        displayLogin();
    }
}
