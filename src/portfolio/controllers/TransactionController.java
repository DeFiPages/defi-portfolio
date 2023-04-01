package portfolio.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import portfolio.models.*;
import portfolio.views.MainView;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.TimeUnit;


public class TransactionController {

    private static TransactionController OBJ = null;

    static {
        OBJ = new TransactionController();
    }

    private SettingsController settingsController = SettingsController.getInstance();
    private CoinPriceController coinPriceController = CoinPriceController.getInstance();
    private String strTransactionData = SettingsController.getInstance().DEFI_PORTFOLIO_HOME + SettingsController.getInstance().strTransactionData;
    private ObservableList<TransactionModel> transactionList;
    private int localBlockCount;
    private final TreeMap<String, TreeMap<String, PortfolioModel>> portfolioList = new TreeMap<>();
    private List<BalanceModel> balanceList = new ArrayList<>();
    private JFrame frameUpdate;
    public JLabel jl;
    public Process defidProcess;
    private Boolean classSingleton = true;
    public TreeMap<String, ImpermanentLossModel> impermanentLossList = new TreeMap<>();
    public TreeMap<String, Double> balanceTreeMap = new TreeMap<>();
    public JSONArray poolPairs;
    public JSONArray tokens;
    public Process ps;

    public TransactionController() {
        if (classSingleton) {
            classSingleton = false;
            updateTransactionList(getLocalTransactionList());
            this.localBlockCount = getLocalBlockCount();
            updateBalanceList();
            getLocalBalanceList();
            calcImpermanentLoss();
        }
    }

    /**
     * Retrieves paged data from the specified URL and combines the results into a single JSONArray.
     * This method handles pagination by checking for a "page" element in the JSON response and
     * making additional requests with the "next" parameter until no more pages are available.
     *
     * @param url The URL to fetch the paged data from. The URL should include the "size" query parameter
     *            to specify the number of items per page,
     *            e.g., "https://ocean.defichain.com/v0/mainnet/poolpairs?size=200".
     * @return A JSONArray containing all the items from all pages of data.
     * @throws IOException If an error occurs while making the HTTP requests or processing the responses.
     */
    public static JSONArray GetAllOceanData(String ocean_url) throws IOException {
        JSONArray allData = new JSONArray();
        String next = null;

        do {
            String urlString = ocean_url;
            if (next != null) {
                urlString += "&next=" + next;
            }
            HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();

            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                JSONObject response = (JSONObject) JSONValue.parse(br.readLine());
                JSONArray data = (JSONArray) response.get("data");
                allData.addAll(data);

                if (response.get("page") != null) {
                    JSONObject page = (JSONObject) response.get("page");
                    next = (String) page.get("next");
                } else {
                    next = null;
                }
            }
        } while (next != null);

        return allData;
    }

    public void calcImpermanentLoss() {

        impermanentLossList.clear();
        for (TransactionModel transaction : transactionList) {
            if ((transaction.typeProperty.getValue().equals("AddPoolLiquidity") | transaction.typeProperty.getValue().equals("RemovePoolLiquidity")) && transaction.cryptoCurrencyProperty.getValue().contains("-")) {

                boolean isDUSDPool = transaction.cryptoCurrencyProperty.getValue().contains("DUSD-DFI");

                TransactionModel coin1 = null;
                TransactionModel coin2 = null;
                for (int i = 0; i < transactionList.size(); i++) {
                    if (transactionList.get(i).blockHeightProperty.getValue() > transaction.blockHeightProperty.getValue())
                        break;

                    if (transactionList.get(i).txIDProperty.getValue().equals(transaction.txIDProperty.getValue()) && !transactionList.get(i).cryptoCurrencyProperty.getValue().contains("-")) {
                        if (coin1 == null && (transactionList.get(i).cryptoCurrencyProperty.getValue().equals("DFI")) || (transactionList.get(i).cryptoCurrencyProperty.getValue().equals("DUSD") && !isDUSDPool)) {
                            coin1 = transactionList.get(i);
                        } else if (coin2 == null) {
                            coin2 = transactionList.get(i);
                        }
                        if (coin1 != null && coin2 != null) {
                            if (!impermanentLossList.containsKey(transaction.cryptoCurrencyProperty.getValue())) {
                                impermanentLossList.put(transaction.cryptoCurrencyProperty.getValue(), new ImpermanentLossModel(coin1.cryptoValueProperty.getValue() * -1, coin2.cryptoValueProperty.getValue() * -1));
                            } else if (transaction.typeProperty.getValue().equals("AddPoolLiquidity")) {
                                impermanentLossList.get(transaction.cryptoCurrencyProperty.getValue()).addPooCoins(coin1.cryptoValueProperty.getValue() * -1, coin2.cryptoValueProperty.getValue() * -1);

                            } else if (transaction.typeProperty.getValue().equals("RemovePoolLiquidity")) {
                                impermanentLossList.get(transaction.cryptoCurrencyProperty.getValue()).removePooCoins(coin1.cryptoValueProperty.getValue(), coin2.cryptoValueProperty.getValue());
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    public void clearTransactionList() {
        transactionList.clear();
    }

    public static TransactionController getInstance() {
        return OBJ;
    }


    public boolean checkRpc() {
        JSONObject jsonObject = getRpcResponse("{\"method\": \"getrpcinfo\"}");
        if (jsonObject != null) {
            return jsonObject.get("result") != null;
        } else {
            return false;
        }
    }

    public void startServer() {
        try {
            if (!this.checkRpc()) {
                switch (this.settingsController.getPlatform()) {
                    case "mac":
                        FileWriter myWriter = new FileWriter(System.getProperty("user.dir").replace("\\", "/") + "/PortfolioData/" + "defi.sh");
                        myWriter.write(this.settingsController.BINARY_FILE_PATH + " -conf=" + this.settingsController.PORTFOLIO_CONFIG_FILE_PATH);
                        myWriter.close();
                        defidProcess = Runtime.getRuntime().exec("/usr/bin/open -a Terminal " + System.getProperty("user.dir").replace("\\", "/") + "/PortfolioData/./" + "defi.sh");
                        break;
                    case "win":
                            String[] commands = {"cmd", "/c", "start", "\"Synchronizing blockchain\"", this.settingsController.BINARY_FILE_PATH, "-conf=" + this.settingsController.PORTFOLIO_CONFIG_FILE_PATH};
                            defidProcess = Runtime.getRuntime().exec(commands);
                        break;
                    case "linux":
                        int notfound = 0;
                        try {
                            defidProcess = Runtime.getRuntime().exec("/usr/bin/x-terminal-emulator -e " + this.settingsController.BINARY_FILE_PATH + " -conf=" + this.settingsController.PORTFOLIO_CONFIG_FILE_PATH);
                        } catch (Exception e) {
                            notfound++;
                        }
                        try {
                            defidProcess = Runtime.getRuntime().exec("/usr/bin/konsole -e " + this.settingsController.BINARY_FILE_PATH + " -conf=" + this.settingsController.PORTFOLIO_CONFIG_FILE_PATH);
                        } catch (Exception e) {
                            notfound++;
                        }
                        if (notfound == 2) {
                            JOptionPane.showMessageDialog(null, "Could not found /usr/bin/x-terminal-emulator or\n /usr/bin/konsole", "Terminal not found", JOptionPane.ERROR_MESSAGE);
                        }
                        break;
                }
            }
        } catch (IOException e) {
            this.settingsController.logger.warning("Exception occured: " + e.toString());
        }
    }

    public void stopServer() {
        try {
            getRpcResponse("{\"method\": \"stop\"}");
            if (defidProcess != null) defidProcess.destroy();
        } catch (Exception e) {
            this.settingsController.logger.warning("Exception occured: " + e.toString());
        }
    }

    public CoinPriceController getCoinPriceController() {
        return coinPriceController;
    }

    public SettingsController getSettingsController() {
        return settingsController;
    }

    public TreeMap<String, TreeMap<String, PortfolioModel>> getPortfolioList() {

        return portfolioList;
    }

    public List<BalanceModel> updateBalanceList() {

        return this.balanceList = getCoinAndTokenBalances();
    }

    public List<BalanceModel> getBalanceList() {
        return this.balanceList;
    }

    public void clearPortfolioList() {
        this.portfolioList.clear();
    }

    public void clearBalanceList() {
        this.balanceList.clear();
    }

    public ObservableList<TransactionModel> getTransactionList() {
        return transactionList;
    }

    public void updateTransactionList(List<TransactionModel> transactionList) {

        if(this.transactionList == null){
            this.transactionList= FXCollections.observableArrayList(transactionList);
        }else{
        this.transactionList.clear();
        this.transactionList.addAll(transactionList);
        }
    }

    public String getBlockCount() {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL("https://ocean.defichain.com/v0/mainnet/stats").openConnection();
            String jsonText = "";
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                jsonText = br.readLine();
            } catch (Exception ex) {
                this.settingsController.logger.warning("Exception occured: " + ex.toString());
            }
            JSONObject obj = (JSONObject) JSONValue.parse(jsonText);
            if (obj.get("data") != null) {
                JSONObject data =  (JSONObject)obj.get("data");
                JSONObject count =  (JSONObject)data.get("count");
                return count.get("blocks").toString();
            } else {
                return "No connection";
            }
        } catch (IOException e) {
            this.settingsController.logger.warning("Exception occured: " + e.toString());
        }

        return "No connection";
    }

    public String getPoolRatio(String poolID, String priceRatio) {
        try {
            if (this.poolPairs == null) {
                this.poolPairs = GetAllOceanData("https://ocean.defichain.com/v0/mainnet/poolpairs?size=200");
            }
            for (Object transaction : this.poolPairs) {
                JSONObject jsonObject = (JSONObject) transaction;
                if (((JSONObject) transaction).get("id").toString().contains(poolID)) {
                    JSONObject ratio = (JSONObject) jsonObject.get("priceRatio");
                    return ((JSONObject) ratio).get(priceRatio).toString();
                }
            }

        } catch (IOException e) {
            this.settingsController.logger.warning("Exception occured: " + e.toString());
        }

        return "-";
    }

    public double getCurrencyFactor(){

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL("https://cdn.jsdelivr.net/gh/fawazahmed0/currency-api@1/latest/currencies/usd/"+SettingsController.getInstance().selectedFiatCurrency.getValue().toLowerCase()+".json").openConnection();
            String jsonText = "";
            String line = "";
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                 while((line=br.readLine()) != null){
                     jsonText = jsonText+line;
                 }
            } catch (Exception ex) {
                this.settingsController.logger.warning("Exception occured: " + ex.toString());
            }
            JSONObject obj = (JSONObject) JSONValue.parse(jsonText);
            if (obj.get(SettingsController.getInstance().selectedFiatCurrency.getValue().toLowerCase()) != null) {

                return Double.parseDouble(obj.get(SettingsController.getInstance().selectedFiatCurrency.getValue().toLowerCase()).toString());

            }

        } catch (IOException e) {
            this.settingsController.logger.warning("Exception occured: " + e.toString());
        }

        return 1;

    }

    public String getPrice(String pool) {
        try {
            JSONArray prices = GetAllOceanData("https://ocean.defichain.com/v0/mainnet/prices?size=200");
            for (Object transaction : prices) {
                JSONObject jsonObject = (JSONObject) transaction;
                if (!pool.contains("DUSD-DFI")) {
                    if (((JSONObject) transaction).get("id").toString().contains(pool.replace("DUSD", "USD"))) {
                        JSONObject price = (JSONObject) jsonObject.get("price");
                        JSONObject aggregated = (JSONObject) price.get("aggregated");
                        String amount = aggregated.get("amount").toString();
                        return amount;
                    }
                } else {
                    return "1";
                }
            }
        } catch (IOException e) {
            this.settingsController.logger.warning("Exception occured: " + e.toString());
        }

        return "No connection";
    }

    public String getBlockCountRpc() {
        try {
            JSONObject jsonObject = getRpcResponse("{\"method\": \"getblockcount\"}");
            if (jsonObject != null) {
                if (jsonObject.get("result") != null) {
                    return jsonObject.get("result").toString();
                } else {
                    return "No connection";
                }
            } else {
                return "No connection";
            }
        } catch (Exception e) {
            this.settingsController.logger.warning("Exception occured: " + e.toString());
            return "No connection";
        }
    }

    public int getAccountHistoryCountRpc() {
        JSONObject jsonObject = getRpcResponse("{\"method\": \"accounthistorycount\",\"params\":[\"mine\"]}");
        return Integer.parseInt(jsonObject.get("result").toString());
    }

    public List<AddressModel> getListAddressGroupingsRpc() {
        List<AddressModel> addressList = new ArrayList<>();
        JSONObject jsonObject = getRpcResponse("{\"method\": \"listaddressgroupings\"}");
        JSONArray transactionJson = (JSONArray) jsonObject.get("result");
        for (Object transaction : (JSONArray) transactionJson.get(0)) {
            addressList.add(getListReceivedByAddress(((JSONArray) transaction).get(0).toString()));
        }
        return addressList;
    }

    public AddressModel getListReceivedByAddress(String address) {

        AddressModel addressModel = null;
        JSONObject jsonObject = getRpcResponse("{\"method\": \"listreceivedbyaddress\",\"params\":[1, false, false, \"" + address + "\"]}");

        if (((JSONArray) jsonObject.get("result")).size() > 0) {
            JSONObject jArray = (JSONObject) (((JSONArray) jsonObject.get("result"))).get(0);
            String[] arr = new String[((JSONArray) (jArray.get("txids"))).size()];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = (String) ((JSONArray) (jArray.get("txids"))).get(i);
            }
            return new AddressModel((String) jArray.get("address"), Double.parseDouble(jArray.get("amount").toString()), Long.parseLong(jArray.get("confirmations").toString()), (String) jArray.get("label"), arr);
        }
        return addressModel;
    }

    public List<TransactionModel> getListAccountHistoryRpc(int depth) {
        JSONObject jsonObject = new JSONObject();
        List<TransactionModel> transactionList = new ArrayList<>();

        try {
            int blockCount = Integer.parseInt(getBlockCountRpc());
            int blockDepth = 10000;
            int firstBlock = 468146;
            int restBlockCount = blockCount + blockDepth + 1;
            for (int i = 0; i < Math.ceil(depth / blockDepth); i = i + 1) {

                double percentage = Math.ceil((((double) (i) * blockDepth) / (double) (depth-firstBlock)) * 100);
                if(percentage>100.0)percentage=100.0;

                try {
                    FileWriter myWriter = new FileWriter(SettingsController.getInstance().DEFI_PORTFOLIO_HOME +  "update.portfolio");
                    myWriter.write(this.settingsController.translationList.getValue().get("UpdateData").toString() + percentage + "%");
                    myWriter.close();
                } catch (IOException e) {
                    this.settingsController.logger.warning("Could not write to update.portfolio.");
                }

                if((blockCount - (i * blockDepth) - i) < firstBlock) break;

                for(Object address: SettingsController.getInstance().listAddresses){
                    jsonObject = getRpcResponse("{\"method\":\"listaccounthistory\",\"params\":[\""+address+"\", {\"maxBlockHeight\":" + (blockCount - (i * blockDepth) - i) + ",\"depth\":" + blockDepth + ",\"no_rewards\":" + false + ",\"limit\":" + blockDepth * 2000 + "}]}");


                JSONArray transactionJson = (JSONArray) jsonObject.get("result");

                for (Object transaction : transactionJson) {
                    JSONObject transactionJ = (JSONObject) transaction;
                    for (String amount : (transactionJ.get("amounts").toString().replace("[", "").replace("]", "").replace("\"", "")).split(",")) {
                        if (transactionJ.get("poolID") != null) {
                            transactionList.add(new TransactionModel(Long.parseLong(transactionJ.get("blockTime").toString()), transactionJ.get("owner").toString(), transactionJ.get("type").toString(), amount, transactionJ.get("blockHash").toString(), Integer.parseInt(transactionJ.get("blockHeight").toString()), transactionJ.get("poolID").toString(), "", transactionJ.get("rewardType").toString() ,this));
                        } else {
                            transactionList.add(new TransactionModel(Long.parseLong(transactionJ.get("blockTime").toString()), transactionJ.get("owner").toString(), transactionJ.get("type").toString(), amount, transactionJ.get("blockHash").toString(), Integer.parseInt(transactionJ.get("blockHeight").toString()), "", transactionJ.get("txid").toString(),transactionJ.get("rewardType").toString(), this));
                        }
                    }
                }
                }
                restBlockCount = blockCount - i * blockDepth;
            }

            restBlockCount = restBlockCount - blockDepth;

            for(Object address: SettingsController.getInstance().listAddresses){
                jsonObject = getRpcResponse("{\"method\":\"listaccounthistory\",\"params\":\""+address+"\", {\"maxBlockHeight\":" + (restBlockCount - 1) + ",\"depth\":" + depth % blockDepth + ",\"no_rewards\":" + false + ",\"limit\":" + (depth % blockDepth) * 2000 + "}]}");

            JSONArray transactionJson = (JSONArray) jsonObject.get("result");
            for (Object transaction : transactionJson) {
                JSONObject transactionJ = (JSONObject) transaction;
                for (String amount : (transactionJ.get("amounts").toString().replace("[", "").replace("]", "").replace("\"", "")).split(",")) {

                    if (transactionJ.get("poolID") != null) {
                        transactionList.add(new TransactionModel(Long.parseLong(transactionJ.get("blockTime").toString()), transactionJ.get("owner").toString(), transactionJ.get("type").toString(), amount, transactionJ.get("blockHash").toString(), Integer.parseInt(transactionJ.get("blockHeight").toString()), transactionJ.get("poolID").toString(), "", transactionJ.get("rewardType").toString(),this));
                    } else {
                        transactionList.add(new TransactionModel(Long.parseLong(transactionJ.get("blockTime").toString()), transactionJ.get("owner").toString(), transactionJ.get("type").toString(), amount, transactionJ.get("blockHash").toString(), Integer.parseInt(transactionJ.get("blockHeight").toString()), "", transactionJ.get("txid").toString(), transactionJ.get("rewardType").toString(),this));
                    }
                }
            }   }
        } catch (Exception e) {
            this.settingsController.logger.warning("Exception occured: " + e.toString());
        }

        return transactionList;
    }

    public void updateJFrame() {
        this.frameUpdate = new JFrame();
        this.frameUpdate.setLayout(null);
        this.frameUpdate.setIconImage(new ImageIcon(System.getProperty("user.dir").replace("\\", "/") + "/defi-portfolio/src/icons/DefiIcon.png").getImage());
        if (this.settingsController.getPlatform().equals("mac")) {
            this.jl = new JLabel(this.settingsController.translationList.getValue().get("InitializingData").toString(), JLabel.CENTER);
        } else {
            ImageIcon icon = new ImageIcon(System.getProperty("user.dir").replace("\\", "/") + "/defi-portfolio/src/icons/ajaxloader.gif");
            this.jl = new JLabel(this.settingsController.translationList.getValue().get("InitializingData").toString(), icon, JLabel.CENTER);
        }
        this.jl.setSize(400, 100);
        this.jl.setLocation(0, 0);
        if (this.settingsController.selectedStyleMode.getValue().equals("Dark Mode")) {
            this.jl.setForeground(Color.WHITE);
        } else {
            this.jl.setForeground(Color.BLACK);
        }
        this.frameUpdate.add(jl);
        this.frameUpdate.setSize(400, 110);
        this.frameUpdate.setLocationRelativeTo(null);
        this.frameUpdate.setUndecorated(true);

        if (this.settingsController.selectedStyleMode.getValue().equals("Dark Mode")) {
            this.frameUpdate.getContentPane().setBackground(new Color(55, 62, 67));
        }
        this.frameUpdate.setVisible(true);
        this.frameUpdate.toFront();

        this.frameUpdate.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                mouseClickPoint = e.getPoint(); // update the position
            }

        });
        this.frameUpdate.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                Point newPoint = e.getLocationOnScreen();
                newPoint.translate(-mouseClickPoint.x, -mouseClickPoint.y); // Moves the point by given values from its location
                frameUpdate.setLocation(newPoint); // set the new location
            }
        });
    }

    private Point mouseClickPoint;

    private JSONObject getRpcResponse(String requestJson) {

        try {
            //URL url = new URL("http://" + this.settingsController.rpcbind + ":" + this.settingsController.rpcport + "/");
            URL url = new URL("http://127.0.0.1:8554");

            HttpURLConnection conn;
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout((int) TimeUnit.MINUTES.toMillis(0L));
            conn.setReadTimeout((int) TimeUnit.MINUTES.toMillis(0L));
            conn.setDoOutput(true);
            conn.setDoInput(true);
            String basicAuth = "Basic " + new String(Base64.getEncoder().encode((this.settingsController.auth.getBytes())));

            conn.setRequestProperty("Authorization", basicAuth);
            conn.setRequestProperty("Content-Type", "application/json-rpc");
            conn.setDoOutput(true);
            conn.getOutputStream().write(requestJson.getBytes(StandardCharsets.UTF_8));
            conn.getOutputStream().close();
            String jsonText = "";
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    jsonText = br.readLine();
                } catch (Exception ex) {
                    this.settingsController.logger.warning("Exception occured: " + ex.toString());
                }
                SettingsController.getInstance().debouncer = true;
                Object obj = JSONValue.parse(jsonText);
                return (JSONObject) obj;

            }

        } catch (IOException ioException) {
            SettingsController.getInstance().runTimer = !(ioException.getMessage().equals("Connection refused: connect") & SettingsController.getInstance().debouncer);
        }


        return new JSONObject();

    }

    public void getLocalBalanceList() {

        File strPortfolioData = new File(this.settingsController.PORTFOLIO_FILE_PATH);


        if (strPortfolioData.exists()) {

            try {
                this.balanceList.clear();
                BufferedReader reader;
                reader = new BufferedReader(new FileReader(
                        strPortfolioData));
                String line = reader.readLine();

                while (line != null) {
                    String[] transactionSplit = line.split(";");
                    BalanceModel balanceModel = new BalanceModel(transactionSplit[0], Double.parseDouble(transactionSplit[2]), Double.parseDouble(transactionSplit[1]), transactionSplit[3], Double.parseDouble(transactionSplit[5]), Double.parseDouble(transactionSplit[4]), Double.parseDouble(transactionSplit[6]));
                    this.balanceList.add(balanceModel);
                    line = reader.readLine();
                }

                reader.close();
            } catch (IOException e) {
                this.settingsController.logger.warning("Exception occured: " + e.toString());
            }
        }
    }

        public List<TransactionModel> getLocalTransactionList() {

        File strPortfolioData = new File(this.strTransactionData);
        List<TransactionModel> transactionList = new ArrayList<>();
        if (strPortfolioData.exists()) {

            try {
                BufferedReader reader;
                reader = new BufferedReader(new FileReader(
                        strPortfolioData));
                String line = reader.readLine();

                while (line != null) {
                    String[] transactionSplit = line.split(";");
                    TransactionModel transAction = new TransactionModel(Long.parseLong(transactionSplit[0]), transactionSplit[1], transactionSplit[2], transactionSplit[3], transactionSplit[4], Integer.parseInt(transactionSplit[5]), transactionSplit[6], transactionSplit[7], transactionSplit[8],this);
                    transactionList.add(transAction);

                    if (transAction.typeProperty.getValue().equals("Rewards") | transAction.typeProperty.getValue().equals("Commission")) {
                        addToPortfolioModel(transAction);
                    }

                    line = reader.readLine();
                }

                reader.close();
                return transactionList;
            } catch (IOException e) {
                this.settingsController.logger.warning("Exception occured: " + e.toString());
            }
        }
        return transactionList;
    }

    public String getTokenFromID(String id) {
        JSONObject jsonObject = getRpcResponse("{\"method\": \"gettoken\",\"params\":[" + id + "]}");
        jsonObject = (JSONObject) jsonObject.get("result");
        jsonObject = (JSONObject) jsonObject.get(id);

        return jsonObject.get("symbol").toString();
    }

    public String getBalance() {
        JSONObject jsonObject = getRpcResponse("{\"method\": \"getbalance\"}");
        return jsonObject.get("result").toString();
    }

    public JSONArray getAddressTokenBalance(String address) {

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL("https://ocean.defichain.com/v0/mainnet/address/"+address+"/tokens").openConnection();
            String jsonText = "";
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                jsonText = br.readLine();
            } catch (Exception ex) {
                this.settingsController.logger.warning("Exception occured: " + ex.toString());
            }

            JSONObject jsonObject = (JSONObject) JSONValue.parse(jsonText);
            JSONArray jsonArray = null;
            if(jsonObject != null){
                jsonArray = (JSONArray) jsonObject.get("data");
            }

            return jsonArray;
        } catch (IOException e) {
            this.settingsController.logger.warning("Exception occured: " + e.toString());
        }
        return null;

    }

    public String getAddressUtxoBalance(String address) {

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL("https://ocean.defichain.com/v0/mainnet/address/"+address+"/balance").openConnection();
            String jsonText = "";
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                jsonText = br.readLine();
            } catch (Exception ex) {
                this.settingsController.logger.warning("Exception occured: " + ex.toString());
            }

            JSONObject jsonObject = (JSONObject) JSONValue.parse(jsonText);

            if(jsonObject == null) return "0";
            String balance = jsonObject.get("data").toString();

            return balance;
        } catch (IOException e) {
            this.settingsController.logger.warning("Exception occured: " + e.toString());
        }
        return null;

    }

    public void addToPortfolioModel(TransactionModel transactionSplit) {

        String pool = getPoolPairFromId(transactionSplit.poolIDProperty.getValue());

        String[] intervallList = new String[]{"Daily", "Weekly", "Monthly", "Yearly"};

        for (String intervall : intervallList) {

            String keyValue = pool + "-" + intervall;

            if (!portfolioList.containsKey(keyValue)) {
                portfolioList.put(keyValue, new TreeMap<>());
            }

            Double newFiatRewards = 0.0;
            Double newFiatCommissions1 = 0.0;
            Double newFiatCommissions2 = 0.0;
            Double newCoinRewards = 0.0;
            Double newCoinCommissions1 = 0.0;
            Double newCoinCommissions2 = 0.0;

            if (transactionSplit.typeProperty.getValue().equals("Rewards")) {
                newFiatRewards = transactionSplit.fiatValueProperty.getValue();
                newCoinRewards = transactionSplit.cryptoValueProperty.getValue();
            }

            if (transactionSplit.typeProperty.getValue().equals("Commission")) {
                try{
                if (pool.split("-")[1].equals(transactionSplit.cryptoCurrencyProperty.getValue())) {
                    newFiatCommissions1 = transactionSplit.fiatValueProperty.getValue();
                    newCoinCommissions1 = transactionSplit.cryptoValueProperty.getValue();
                } else {
                    newFiatCommissions2 = transactionSplit.fiatValueProperty.getValue();
                    newCoinCommissions2 = transactionSplit.cryptoValueProperty.getValue();
                }}catch(Exception ex){
                    this.settingsController.logger.warning("Exception occured: " + ex.toString());
                }
            }

            if (portfolioList.get(keyValue).containsKey(getDate(Long.toString(transactionSplit.blockTimeProperty.getValue()), intervall))) {

                Double oldCoinRewards = portfolioList.get(keyValue).get(getDate(Long.toString(transactionSplit.blockTimeProperty.getValue()), intervall)).getCoinRewards1Value();
                Double oldFiatRewards = portfolioList.get(keyValue).get(getDate(Long.toString(transactionSplit.blockTimeProperty.getValue()), intervall)).getFiatRewards1Value();
                Double oldCoinCommissions1 = portfolioList.get(keyValue).get(getDate(Long.toString(transactionSplit.blockTimeProperty.getValue()), intervall)).getCoinCommissions1Value();
                Double oldFiatCommissions1 = portfolioList.get(keyValue).get(getDate(Long.toString(transactionSplit.blockTimeProperty.getValue()), intervall)).getFiatCommissions1Value();
                Double oldCoinCommissions2 = portfolioList.get(keyValue).get(getDate(Long.toString(transactionSplit.blockTimeProperty.getValue()), intervall)).getCoinCommissions2Value();
                Double oldFiatCommissions2 = portfolioList.get(keyValue).get(getDate(Long.toString(transactionSplit.blockTimeProperty.getValue()), intervall)).getFiatCommissions2Value();
                portfolioList.get(keyValue).put(getDate(Long.toString(transactionSplit.blockTimeProperty.getValue()), intervall), new PortfolioModel(getDate(Long.toString(transactionSplit.blockTimeProperty.getValue()), intervall), oldFiatRewards + newFiatRewards, oldFiatCommissions1 + newFiatCommissions1, oldFiatCommissions2 + newFiatCommissions2, oldCoinRewards + newCoinRewards, oldCoinCommissions1 + newCoinCommissions1, oldCoinCommissions2 + newCoinCommissions2, pool, intervall));
            } else {
                portfolioList.get(keyValue).put(getDate(Long.toString(transactionSplit.blockTimeProperty.getValue()), intervall), new PortfolioModel(getDate(Long.toString(transactionSplit.blockTimeProperty.getValue()), intervall), newFiatRewards, newFiatCommissions1, newFiatCommissions2, newCoinRewards, newCoinCommissions1, newCoinCommissions2, pool, intervall));
            }
        }
    }

    public String getDate(String blockTime, String intervall) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(Long.parseLong(blockTime) * 1000L);
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int week = cal.get(Calendar.WEEK_OF_YEAR);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        String date = "";

        if (SettingsController.getInstance().translationList.getValue().get("Daily").equals(intervall) | intervall.equals("Daily")) {
            String monthAdapted = Integer.toString(month);
            if (month < 10) {
                monthAdapted = "0" + month;
            }
            if (day < 10) {
                date = year + "-" + monthAdapted + "-0" + day;
            } else {
                date = year + "-" + monthAdapted + "-" + day;
            }
        }

        if (SettingsController.getInstance().translationList.getValue().get("Weekly").equals(intervall) | intervall.equals("Weekly")) {
            int correct = 0;
            if (month == 1 && (day == 1 || day == 2 || day == 3)) {
                correct = 1;
            }
            if (week < 10) {
                date = year - correct + "-0" + week;
            } else {
                date = year - correct + "-" + week;
            }
        }

        if (SettingsController.getInstance().translationList.getValue().get("Monthly").equals(intervall) | intervall.equals("Monthly")) {
            if (month < 10) {
                date = year + "-0" + month;
            } else {
                date = year + "-" + month;
            }
        }

        if (SettingsController.getInstance().translationList.getValue().get("Yearly").equals(intervall) | intervall.equals("Yearly")) {

            date = Integer.toString(year);
        }
        return date;
    }

    public String convertDateToIntervall(String strDate, String intervall) {
        Date date = null;
        try {
            date = new SimpleDateFormat("yyyy-MM-dd").parse(strDate);
        } catch (ParseException e) {
            this.settingsController.logger.warning("Exception occured: " + e.toString());
        }
        assert date != null;
        Timestamp ts = new Timestamp(date.getTime());
        return this.getDate(Long.toString(ts.getTime() / 1000), intervall);
    }

    public int getLocalBlockCount() {
        if (!new File(SettingsController.getInstance().DEFI_PORTFOLIO_HOME + "/transactionData.portfolio").exists()) {
            return 0;
        }
        if (transactionList.size() > 0) {

            return transactionList.get(transactionList.size() - 1).blockHeightProperty.getValue();
        } else {
            return 0;
        }
    }
    public String getPoolPairFromId(String poolID) {
        String pool = "-";

        if (!poolID.isEmpty() && !poolID.contains("_") && !poolID.contains("-")) {
            if (Integer.parseInt(poolID) <= 14) {

                switch (poolID) {
                    case "0":
                    case "0.0":
                        pool = "DFI";
                        break;
                    case "1":
                    case "1.0":
                        pool = "ETH";
                        break;
                    case "2":
                    case "2.0":
                        pool = "BTC";
                        break;
                    case "3":
                    case "3.0":
                        pool = "USDT";
                        break;
                    case "4":
                    case "4.0":
                        pool = "ETH-DFI";
                        break;
                    case "5":
                    case "5.0":
                        pool = "BTC-DFI";
                        break;
                    case "6":
                    case "6.0":
                        pool = "USDT-DFI";
                        break;
                    case "7":
                    case "7.0":
                        pool = "DOGE";
                        break;
                    case "8":
                    case "8.0":
                        pool = "DOGE-DFI";
                        break;
                    case "9":
                    case "9.0":
                        pool = "LTC";
                        break;
                    case "10":
                    case "10.0":
                        pool = "LTC-DFI";
                        break;
                    case "11":
                    case "11.0":
                        pool = "BCH";
                        break;
                    case "12":
                    case "12.0":
                        pool = "BCH-DFI";
                        break;
                    case "13":
                    case "13.0":
                        pool = "USDC";
                        break;
                    case "14":
                    case "14.0":
                        pool = "USDC-DFI";
                        break;
                    default:
                        pool = "-";
                        break;
                }
            } else {
                try {
                    if (this.tokens == null) {
                        this.tokens = GetAllOceanData("https://ocean.defichain.com/v0/mainnet/tokens?size=200");
                    }

                    for (Object token : this.tokens) {
                        JSONObject jsonToken = (JSONObject) token;
                        if (jsonToken.get("id").toString().equals(poolID)) {
                            pool = jsonToken.get("symbol").toString();
                        }
                    }
                } catch (IOException e) {
                    this.settingsController.logger.warning("Exception occured: " + e.toString());
                }
            }
        }
        return pool;
    }

    public String getIdFromPoolPair(String poolID) {
        String pool= "-";

        try {
            if (this.tokens == null) {
                this.tokens = GetAllOceanData("https://ocean.defichain.com/v0/mainnet/tokens?size=200");
            }
            for (Object token : this.tokens) {
                JSONObject jsonToken = (JSONObject) token;
                if (jsonToken.get("symbol").toString().contains(poolID)) {
                    return pool = jsonToken.get("id").toString();
                }
            }
        } catch (IOException e) {
            this.settingsController.logger.warning("Exception occured: " + e.toString());
        }
        return pool;
    }

    public List<BalanceModel> getCoinAndTokenBalances() {
        TreeMap<String, Double> balanceTreeMap = new TreeMap<>();
        List<BalanceModel> balanceModelList = new ArrayList<>();
        JSONArray jsonArray = new JSONArray();
        for (String address:
                SettingsController.getInstance().listAddresses) {
             jsonArray = getAddressTokenBalance(address);

             if(jsonArray!=null) {
                 for (Object token : jsonArray) {
                     JSONObject jsonToken = (JSONObject) token;
                     String tokenName = jsonToken.get("symbol").toString();
                     double tokenValue = Double.parseDouble(jsonToken.get("amount").toString());

                     if (!balanceTreeMap.containsKey(tokenName)) {
                         balanceTreeMap.put(tokenName, tokenValue);
                     } else {
                         double oldValue = balanceTreeMap.get(tokenName).doubleValue();
                         balanceTreeMap.put(tokenName, oldValue + tokenValue);
                     }
                 }

                 if (!balanceTreeMap.containsKey("DFI")) {
                     balanceTreeMap.put("DFI", 0.0);
                 }
                 double oldValue = balanceTreeMap.get("DFI").doubleValue();
                 double balanceValue = Double.parseDouble(getAddressUtxoBalance(address));
                 balanceTreeMap.put("DFI", oldValue + balanceValue);
             }
        }


                for(Map.Entry<String,Double> entry : balanceTreeMap.entrySet()) {


                if (entry.getKey().contains("-")) {
                    Double poolRatio = Double.parseDouble(getPoolRatio(this.getIdFromPoolPair(entry.getKey()),"ab"));
                    Double token1 = Math.sqrt(poolRatio * entry.getValue() * entry.getValue());
                    Double token2 = Math.sqrt(entry.getValue() * entry.getValue() / poolRatio);
                    try {
                        balanceModelList.add(new BalanceModel(entry.getKey().split("-")[0], coinPriceController.getPriceFromTimeStamp(entry.getKey().contains("DUSD"),entry.getKey().split("-")[0] + SettingsController.getInstance().selectedFiatCurrency.getValue(), System.currentTimeMillis()) * token1, token1, entry.getKey().split("-")[1], coinPriceController.getPriceFromTimeStamp(entry.getKey().split("-")[1].contains("DUSD"),entry.getKey().split("-")[1] + SettingsController.getInstance().selectedFiatCurrency.getValue(), System.currentTimeMillis()) * token2, token2, entry.getValue()));
                    } catch (Exception e) {
                        this.settingsController.logger.warning("Exception occured: " + e.toString());
                    }
                } else {
                    if (entry.getValue()>0) {
                        balanceModelList.add(new BalanceModel(entry.getKey(), coinPriceController.getPriceFromTimeStamp(entry.getKey().contains("DUSD") ,entry.getKey() + SettingsController.getInstance().selectedFiatCurrency.getValue(), System.currentTimeMillis()) * entry.getValue(), entry.getValue() ,
                                "-", 0.0, 0.0, 0.0));
                    }
                }
            }

            if (balanceModelList.size() > 0) {
                try {
                    PrintWriter writer = new PrintWriter(new FileWriter(this.settingsController.PORTFOLIO_FILE_PATH));
                    String exportSplitter = ";";
                    StringBuilder sb = new StringBuilder();

                    for (BalanceModel balanceModel : balanceModelList) {
                        sb.append(balanceModel.getToken1NameValue()).append(exportSplitter);
                        sb.append(balanceModel.getCrypto1Value()).append(exportSplitter);
                        sb.append(balanceModel.getFiat1Value()).append(exportSplitter);
                        sb.append(balanceModel.getToken2NameValue()).append(exportSplitter);
                        sb.append(balanceModel.getCrypto2Value()).append(exportSplitter);
                        sb.append(balanceModel.getFiat2Value()).append(exportSplitter);
                        sb.append(balanceModel.getShareValue()).append(exportSplitter);
                        sb.append("\n");
                    }

                    writer.write(sb.toString());
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        return balanceModelList;
    }

    public List<TransactionModel> getTransactionsInTime(List<TransactionModel> transactions, long startTime, long endTime) {
        List<TransactionModel> filteredTransactions = new ArrayList<>();
        for (int ilist = 1; ilist < transactions.size(); ilist++) {
            if (transactions.get(ilist).blockTimeProperty.getValue() >= startTime && transactions.get(ilist).blockTimeProperty.getValue() <= endTime) {
                filteredTransactions.add(transactions.get(ilist));
            }
        }
        return filteredTransactions;
    }

    public List<TransactionModel> getTransactionsOfType(List<TransactionModel> transactions, String type) {
        List<TransactionModel> filteredTransactions = new ArrayList<>();
        for (int ilist = 1; ilist < transactions.size(); ilist++) {
            if (transactions.get(ilist).typeProperty.getValue().equals(type)) {
                filteredTransactions.add(transactions.get(ilist));
            }
        }
        return filteredTransactions;
    }

    public List<TransactionModel> getTransactionsOfOwner(List<TransactionModel> transactions, String owner) {
        List<TransactionModel> filteredTransactions = new ArrayList<>();
        for (int ilist = 1; ilist < transactions.size(); ilist++) {
            if (transactions.get(ilist).typeProperty.getValue().equals(owner)) {
                filteredTransactions.add(transactions.get(ilist));
            }
        }
        return filteredTransactions;
    }

    public List<TransactionModel> getTransactionsBetweenBlocks(List<TransactionModel> transactions, long startBlock, long endBlock) {
        List<TransactionModel> filteredTransactions = new ArrayList<>();
        for (int ilist = 1; ilist < transactions.size(); ilist++) {
            if (transactions.get(ilist).blockHeightProperty.getValue() >= startBlock && transactions.get(ilist).blockHeightProperty.getValue() <= endBlock) {
                filteredTransactions.add(transactions.get(ilist));
            }
        }
        return filteredTransactions;
    }

    public String convertTimeStampToString(long timeStamp) {
        Date date = new Date(timeStamp * 1000L);
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(date);
    }

    public String convertTimeStampYesterdayToString(long timeStamp) {
        Date date = new Date(timeStamp * 1000L);
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd 23:59:59");
        return dateFormat.format(date);
    }

    public String convertTimeStampWithoutTimeToString(long timeStamp) {
        Date date = new Date(timeStamp * 1000L);
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T00:00:00'");
        return dateFormat.format(date);
    }

    public String convertTimeStampToCointracking(long timeStamp) {
        Date date = new Date(timeStamp * 1000L);
        DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy 00:00:00");
        return dateFormat.format(date);
    }

    public String convertTimeStampToCointrackingReal(long timeStamp) {
        Date date = new Date(timeStamp * 1000L);
        DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        return dateFormat.format(date);
    }

    public double getTotalCoinAmount(List<TransactionModel> transactions, String coinName) {

        double amountCoin = 0;

        for (TransactionModel transaction : transactions) {

            if (!transaction.typeProperty.getValue().equals("UtxosToAccount") & !transaction.typeProperty.getValue().equals("AccountToUtxos")) {
                String[] CoinsAndAmounts = splitCoinsAndAmounts(transaction.amountProperty.getValue());
                if (coinName.equals(CoinsAndAmounts[1])) {
                    amountCoin += Double.parseDouble(CoinsAndAmounts[0]);
                }

            }
        }
        return amountCoin;
    }

    public String[] splitCoinsAndAmounts(String amountAndCoin) {
        return amountAndCoin.split("@");
    }

    static class Delta {
        double x, y;
    }

    public void updateDatabase(){

        MainViewController.getInstance().settingsController.selectedLaunchSync = true;
        MainViewController.getInstance().transactionController.startServer();
        MainViewController.getInstance().settingsController.runCheckTimer = true;
        Timer checkTimer = new Timer("");

        try {
            FileWriter myWriter = new FileWriter(SettingsController.getInstance().DEFI_PORTFOLIO_HOME +  "update.portfolio");
            myWriter.write(MainViewController.getInstance().settingsController.translationList.getValue().get("ConnectNode").toString());
            myWriter.close();

            if(SettingsController.getInstance().getPlatform().contains("mac")){
            try {
                this.ps = null;
                this.ps = Runtime.getRuntime().exec("./jre/bin/java -Xdock:icon=icons.icns -jar UpdateData.jar " + MainViewController.getInstance().settingsController.selectedStyleMode.getValue().replace(" ", ""));
            } catch (IOException r) {
                SettingsController.getInstance().logger.warning("Exception occured: " + r.toString());
            }}
            if(SettingsController.getInstance().getPlatform().contains("linux")) {
                try {
                    this.ps = null;
                    this.ps = Runtime.getRuntime().exec("jre/bin/java -jar UpdateData.jar " + MainViewController.getInstance().settingsController.selectedStyleMode.getValue().replace(" ", ""));
                } catch (IOException r) {
                    SettingsController.getInstance().logger.warning("Exception occured: " + r.toString());
                }
            }
            else{
                try {
                    this.ps = null;
                    this.ps = Runtime.getRuntime().exec("./jre/bin/java -jar UpdateData.jar " + MainViewController.getInstance().settingsController.selectedStyleMode.getValue().replace(" ", ""));
                } catch (IOException r) {
                    SettingsController.getInstance().logger.warning("Exception occured: " + r.toString());
                }
            }
        } catch (IOException h) {
            SettingsController.getInstance().logger.warning("Could not write to update.portfolio.");
        }

        checkTimer.scheduleAtFixedRate(new CheckConnection(MainViewController.getInstance()), 0, 30000);
    }

    public void importCakeCSV(){
        System.out.println("cake import");
    }


    public void importWalletCSV(){
        FileChooser fileChooser = new FileChooser();

        FileChooser.ExtensionFilter extFilter =
                new FileChooser.ExtensionFilter("DeFi Wallet CSV (*.csv)", "*.csv");
        fileChooser.getExtensionFilters().add(extFilter);
        Path path = Paths.get(this.settingsController.lastWalletCSVImportPath);
        if(this.settingsController.lastWalletCSVImportPath != null && !this.settingsController.lastWalletCSVImportPath.isEmpty() && Files.exists(path)){
            fileChooser.setInitialDirectory(new File(this.settingsController.lastWalletCSVImportPath));
        }else{
            fileChooser.setInitialDirectory(new File(this.settingsController.DEFI_PORTFOLIO_HOME));
        }

        Label fileLabel = new Label();
        List<File> list = fileChooser.showOpenMultipleDialog(new Stage());
        //File file = fileChooser.showOpenDialog(new Stage());
        if (list != null) {
            // Save latest path in settings
            this.settingsController.lastWalletCSVImportPath = list.get(0).getParent().toString().replace("\\","/");
            this.settingsController.saveSettings();
            // import csv data
            getLocalWalletCSVList(list);
            //updateBalanceList();
            //getLocalBalanceList();
            //calcImpermanentLoss();
            //MainViewController.getInstance().plotUpdate(MainViewController.getInstance().mainView.tabPane.getSelectionModel().getSelectedItem().getId());
        }
    }

    //public ObservableList<TransactionModel> getLocalWalletCSVList(String filePath) {
    public void getLocalWalletCSVList(List<File> files) {
        int iFile = 1;
        //this.frameUpdate.setAlwaysOnTop(true);
        for (File strPortfolioData : files) {
            File file = new File(SettingsController.getInstance().DEFI_PORTFOLIO_HOME+"\\CSVMerge.cookie");
            if(!file.exists()){
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (strPortfolioData.exists()) {
                try {
                    // Start skript
                    switch (this.settingsController.getPlatform()) {
                        case "mac":
                            String pathMac = System.getProperty("user.dir")+"\\defi-portfolio\\src\\portfolio\\libraries\\main ";
                            String[] commandsMac = {"cmd", "/c", "start", "\"Merging data\"", pathMac,SettingsController.getInstance().DEFI_PORTFOLIO_HOME.replace("/","\\")+"transactionData.portfolio",strPortfolioData.getAbsolutePath()};
                            defidProcess = Runtime.getRuntime().exec("/usr/bin/open -a Terminal " +pathMac + SettingsController.getInstance().DEFI_PORTFOLIO_HOME+"transactionData.portfolio " + strPortfolioData.getAbsolutePath());
                            break;
                        case "win":
                            String path = System.getProperty("user.dir")+"\\defi-portfolio\\src\\portfolio\\libraries\\main.exe";
                            String[] commands = {"cmd", "/c", "start", "\"Merging data\"", path,SettingsController.getInstance().DEFI_PORTFOLIO_HOME.replace("/","\\")+"transactionData.portfolio",strPortfolioData.getAbsolutePath()};
                            defidProcess = Runtime.getRuntime().exec(commands);
                            break;
                        case "linux":
                            String pathlinux = System.getProperty("user.dir")+"/defi-portfolio/src/portfolio/libraries/main ";
                            SettingsController.getInstance().logger.warning("/usr/bin/x-terminal-emulator -e "  + pathlinux + SettingsController.getInstance().DEFI_PORTFOLIO_HOME+"transactionData.portfolio " + strPortfolioData.getAbsolutePath());
                            int notfound = 0;
                            try {
                                defidProcess = Runtime.getRuntime().exec("/usr/bin/x-terminal-emulator -e "  + pathlinux + SettingsController.getInstance().DEFI_PORTFOLIO_HOME+"transactionData.portfolio " + strPortfolioData.getAbsolutePath());
                            } catch (Exception e) {
                                notfound++;
                            }
                            try {
                                defidProcess = Runtime.getRuntime().exec("/usr/bin/konsole -e "  +"wine "+  pathlinux + SettingsController.getInstance().DEFI_PORTFOLIO_HOME+"transactionData.portfolio " + strPortfolioData.getAbsolutePath());
                            } catch (Exception e) {
                                notfound++;
                            }
                            if (notfound == 2) {
                                JOptionPane.showMessageDialog(null, "Could not found /usr/bin/x-terminal-emulator or\n /usr/bin/konsole", "Terminal not found", JOptionPane.ERROR_MESSAGE);
                            }
                            break;
                    }

                    while(file.exists()){
                        Thread.sleep(1000);
                    }

                } catch (Exception e) {
                    this.settingsController.logger.warning("Exception occured: " + e.toString());
                }
            }
            iFile++;
        }

        File f = new File(SettingsController.getInstance().DEFI_PORTFOLIO_HOME.replace("/","\\")+"MergingErrorOccured.txt");
        if (f.exists()){
            f.delete();
            MainView.getInstance().showFileTypeNotSupported();
        }else{
            MainView.getInstance().showRestartWindow();
        }

    }

    public String convertWalletDateToTimeStamp(String input){
        Date date = null;
        try {
            date = new SimpleDateFormat("dd/MM/yyyy / hh:mm a").parse(input);
        } catch (ParseException e) {
            this.settingsController.logger.warning("Exception occured: " + e.toString());
        }
        assert date != null;
        Timestamp ts = new Timestamp(date.getTime());
        return Long.toString(ts.getTime() / 1000);
    }
}