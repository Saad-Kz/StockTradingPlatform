import java.io.*;
import java.util.*;

/* ================================
   MAIN CLASS
   ================================ */
public class TradingPlatform {

    private static final Scanner sc = new Scanner(System.in);
    private final Market market = new Market();
    private User user;

    public static void main(String[] args) {
        new TradingPlatform().start();
    }

    public void start() {
        System.out.println("=== Simple Stock Trading Platform ===");
        System.out.print("Enter username: ");
        String uname = sc.nextLine();

        try {
            Portfolio portfolio = Portfolio.loadFromFile(uname);
            user = new User(uname, portfolio);
            user.loadTransactions();
            System.out.println("Loaded existing user.");
        } catch (Exception e) {
            user = new User(uname, new Portfolio(10000));
            System.out.println("New user created.");
        }

        boolean running = true;
        while (running) {
            showMenu();
            switch (sc.nextLine()) {
                case "1": market.displayMarket(); break;
                case "2": market.simulateMarketTick(); break;
                case "3": buy(); break;
                case "4": sell(); break;
                case "5": showPortfolio(); break;
                case "6": showTransactions(); break;
                case "7": save(); break;
                case "0": save(); running = false; break;
            }
        }
    }

    private void showMenu() {
        System.out.println("\n1) Show Market");
        System.out.println("2) Update Market");
        System.out.println("3) Buy Stock");
        System.out.println("4) Sell Stock");
        System.out.println("5) Portfolio");
        System.out.println("6) Transactions");
        System.out.println("7) Save");
        System.out.println("0) Exit");
        System.out.print("Choose: ");
    }

    private void buy() {
        System.out.print("Symbol: ");
        String s = sc.nextLine().toUpperCase();
        Stock stock = market.getStock(s);
        if (stock == null) return;

        System.out.print("Qty: ");
        int qty = Integer.parseInt(sc.nextLine());

        if (user.getPortfolio().buy(s, qty, stock.getPrice())) {
            Transaction tx = new Transaction(Transaction.Type.BUY, s, qty, stock.getPrice());
            user.recordTransaction(tx);
            System.out.println("Bought: " + tx);
        } else System.out.println("Not enough cash.");
    }

    private void sell() {
        System.out.print("Symbol: ");
        String s = sc.nextLine().toUpperCase();
        Stock stock = market.getStock(s);
        if (stock == null) return;

        System.out.print("Qty: ");
        int qty = Integer.parseInt(sc.nextLine());

        if (user.getPortfolio().sell(s, qty, stock.getPrice())) {
            Transaction tx = new Transaction(Transaction.Type.SELL, s, qty, stock.getPrice());
            user.recordTransaction(tx);
            System.out.println("Sold: " + tx);
        } else System.out.println("Not enough shares.");
    }

    private void showPortfolio() {
        System.out.println("Cash: $" + user.getPortfolio().getCash());
        System.out.println("Total Value: $" + user.getPortfolio().portfolioValue(market));
    }

    private void showTransactions() {
        for (Transaction t : user.getTransactions()) System.out.println(t);
    }

    private void save() {
        try {
            user.getPortfolio().saveToFile(user.getUsername());
            user.saveTransactionsToFile();
            System.out.println("Saved.");
        } catch (Exception e) {
            System.out.println("Error saving!");
        }
    }
}

/* ================================
   MARKET CLASS
   ================================ */
class Market {
    private Map<String, Stock> stocks = new HashMap<>();

    public Market() {
        stocks.put("AAPL", new Stock("AAPL", 180));
        stocks.put("GOOG", new Stock("GOOG", 120));
        stocks.put("TSLA", new Stock("TSLA", 200));
    }

    public void displayMarket() {
        System.out.println("\n=== Market Prices ===");
        for (Stock s : stocks.values()) System.out.println(s);
    }

    public void simulateMarketTick() {
        for (Stock s : stocks.values()) s.updatePrice();
        System.out.println("Market updated.");
    }

    public Stock getStock(String symbol) {
        return stocks.get(symbol);
    }
}

/* ================================
   STOCK CLASS
   ================================ */
class Stock {
    private String symbol;
    private double price;

    public Stock(String symbol, double price) {
        this.symbol = symbol;
        this.price = price;
    }

    public void updatePrice() {
        price += (Math.random() * 10 - 5);
        if (price < 1) price = 1;
    }

    public String getSymbol() { return symbol; }
    public double getPrice() { return price; }

    public String toString() {
        return symbol + " : $" + price;
    }
}

/* ================================
   PORTFOLIO CLASS
   ================================ */
class Portfolio {

    private Map<String, Integer> holdings = new HashMap<>();
    private double cash;

    public Portfolio(double cash) {
        this.cash = cash;
    }

    public boolean buy(String stock, int qty, double price) {
        double cost = qty * price;
        if (cost > cash) return false;
        cash -= cost;
        holdings.put(stock, holdings.getOrDefault(stock, 0) + qty);
        return true;
    }

    public boolean sell(String stock, int qty, double price) {
        if (!holdings.containsKey(stock) || holdings.get(stock) < qty) return false;
        cash += qty * price;
        int left = holdings.get(stock) - qty;
        if (left > 0) holdings.put(stock, left);
        else holdings.remove(stock);
        return true;
    }

    public double getCash() { return cash; }

    public double portfolioValue(Market market) {
        double total = cash;
        for (String s : holdings.keySet()) {
            Stock stock = market.getStock(s);
            if (stock != null)
                total += holdings.get(s) * stock.getPrice();
        }
        return total;
    }

    public void saveToFile(String username) throws IOException {
        PrintWriter pw = new PrintWriter(new FileWriter(username + "_portfolio.txt"));
        pw.println(cash);
        for (String s : holdings.keySet())
            pw.println(s + "," + holdings.get(s));
        pw.close();
    }

    public static Portfolio loadFromFile(String username) throws Exception {
        File f = new File(username + "_portfolio.txt");
        if (!f.exists()) throw new FileNotFoundException();

        BufferedReader br = new BufferedReader(new FileReader(f));
        double cash = Double.parseDouble(br.readLine());
        Portfolio p = new Portfolio(cash);

        String line;
        while ((line = br.readLine()) != null) {
            String[] a = line.split(",");
            p.holdings.put(a[0], Integer.parseInt(a[1]));
        }

        br.close();
        return p;
    }
}

/* ================================
   USER CLASS
   ================================ */
class User {

    private String username;
    private Portfolio portfolio;
    private List<Transaction> transactions = new ArrayList<>();

    public User(String username, Portfolio portfolio) {
        this.username = username;
        this.portfolio = portfolio;
    }

    public String getUsername() { return username; }
    public Portfolio getPortfolio() { return portfolio; }
    public List<Transaction> getTransactions() { return transactions; }

    public void recordTransaction(Transaction t) {
        transactions.add(t);
    }

    public void saveTransactionsToFile() throws IOException {
        PrintWriter pw = new PrintWriter(new FileWriter(username + "_transactions.txt"));
        for (Transaction t : transactions)
            pw.println(t.toFileString());
        pw.close();
    }

    public void loadTransactions() throws IOException {
        File f = new File(username + "_transactions.txt");
        if (!f.exists()) return;

        BufferedReader br = new BufferedReader(new FileReader(f));
        String line;
        while ((line = br.readLine()) != null) {
            transactions.add(Transaction.fromFileString(line));
        }
        br.close();
    }
}

/* ================================
   TRANSACTION CLASS
   ================================ */
class Transaction {

    enum Type { BUY, SELL }

    private Type type;
    private String symbol;
    private int qty;
    private double price;

    public Transaction(Type type, String symbol, int qty, double price) {
        this.type = type;
        this.symbol = symbol;
        this.qty = qty;
        this.price = price;
    }

    public String toFileString() {
        return type + "," + symbol + "," + qty + "," + price;
    }

    public static Transaction fromFileString(String s) {
        String[] a = s.split(",");
        return new Transaction(
                Type.valueOf(a[0]),
                a[1],
                Integer.parseInt(a[2]),
                Double.parseDouble(a[3])
        );
    }

    public String toString() {
        return type + " " + qty + " of " + symbol + " @ $" + price;
    }
}
