package portfolio.controllers;
import com.sun.javafx.charts.Legend;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import portfolio.models.*;
import portfolio.services.ExportService;
import portfolio.views.MainView;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;

public class MainViewController {

    public StringProperty strCurrentBlockLocally = new SimpleStringProperty("0");
    public StringProperty strCurrentBlockOnBlockchain = new SimpleStringProperty("No connection");
    public StringProperty strProgressbar = new SimpleStringProperty("");
    public BooleanProperty bDataBase = new SimpleBooleanProperty(true);

    //View
    public MainView mainView;

    //Table and plot lists
    public List<PoolPairModel> poolPairModelList = new ArrayList<>();
    public ObservableList<PoolPairModel> poolPairList;

    //Init all controller and services
    public SettingsController settingsController = SettingsController.getInstance();
    public DonateController donateController = DonateController.getInstance();
    public HelpController helpController = HelpController.getInstance();
    public CoinPriceController coinPriceController = CoinPriceController.getInstance();
    public TransactionController transactionController = TransactionController.getInstance();
    public ExportService expService;
    public boolean updateSingleton = true;
    final Delta dragDelta = new Delta();
    public Process defidProcess;
    private static MainViewController OBJ;

    static {
        OBJ = new MainViewController();
    }

    public static MainViewController getInstance() {
        return OBJ;
    }

    public MainViewController() {

        this.settingsController.logger.info("Start DeFi-Portfolio");
        if (this.settingsController.selectedLaunchDefid) {
            if (!this.transactionController.checkRpc()) this.transactionController.startServer();
        }

        this.startStockUpdate();

        // init all relevant lists for tables and plots
        this.poolPairList = FXCollections.observableArrayList(this.poolPairModelList);
        this.expService = new ExportService(this);
        this.coinPriceController.updateCoinPriceData();
        this.coinPriceController.updateStockPriceData();

        // get last block locally
        this.strCurrentBlockLocally.set(Integer.toString(transactionController.getLocalBlockCount()));

        //Add listener to Fiat
        this.settingsController.selectedFiatCurrency.addListener(
                (ov, t, t1) -> {
                    this.transactionController.getPortfolioList().clear();
                    for (TransactionModel transactionModel : this.transactionController.getTransactionList()) {
                        if (!transactionModel.cryptoCurrencyProperty.getValue().contains("-")) {
                            transactionModel.fiatCurrencyProperty.set(t1);
                            transactionModel.fiatValueProperty.set(transactionModel.cryptoValueProperty.getValue() * this.coinPriceController.getPriceFromTimeStamp(transactionModel.cryptoCurrencyProperty.getValue().contains("DUSD"),transactionModel.cryptoCurrencyProperty.getValue() + t1, transactionModel.blockTimeProperty.getValue() * 1000L));
                        }

                        if (transactionModel.typeProperty.getValue().equals("Rewards") | transactionModel.typeProperty.getValue().equals("Commission")) {
                            this.transactionController.addToPortfolioModel(transactionModel);
                        }
                    }
                    for (BalanceModel balanceModel : this.transactionController.getBalanceList()) {
                        balanceModel.setFiat1(balanceModel.getCrypto1Value() * CoinPriceController.getInstance().getPriceFromTimeStamp(balanceModel.getToken1NameValue().contains("DUSD"),balanceModel.getToken1NameValue() + SettingsController.getInstance().selectedFiatCurrency.getValue(), System.currentTimeMillis()));
                        balanceModel.setFiat2(balanceModel.getCrypto2Value() * CoinPriceController.getInstance().getPriceFromTimeStamp(balanceModel.getToken2NameValue().contains("DUSD"),balanceModel.getToken2NameValue() + SettingsController.getInstance().selectedFiatCurrency.getValue(), System.currentTimeMillis()));
                    }
                }
        );
        startTimer();
    }

    public void startStockUpdate(){
        //Start Python update

        try {
            File f = new File(SettingsController.getInstance().DEFI_PORTFOLIO_HOME +"StockPricesPythonUpdate.portfolio");
            f.createNewFile();
        } catch (Exception e) {
            SettingsController.getInstance().logger.warning("Could not write python update file."); }

        try {
            // Start skript
            switch (this.settingsController.getPlatform()) {
                case "mac":
                    // defidProcess = Runtime.getRuntime().exec("/usr/bin/open -a Terminal " + System.getProperty("user.dir").replace("\\", "/") + "/PortfolioData/./" + "defi.sh");
                    Runtime.getRuntime().exec("/usr/bin/open -a Terminal " + SettingsController.getInstance().DEFI_PORTFOLIO_HOME + "StockTokenPrices");
                    break;
                case "win":
                    String path = System.getProperty("user.dir")+"\\defi-portfolio\\src\\portfolio\\libraries\\StockTokenPrices.exe";
                    String[] commands = {"cmd", "/c", "start", "\"Update Portfolio\"", path,SettingsController.getInstance().DEFI_PORTFOLIO_HOME};
                    defidProcess = Runtime.getRuntime().exec(commands);
                    break;
                case "linux":
                    String pathlinux = System.getProperty("user.dir")+"/defi-portfolio/src/portfolio/libraries/StockTokenPrices ";
                    SettingsController.getInstance().logger.warning(pathlinux +" '"+ SettingsController.getInstance().DEFI_PORTFOLIO_HOME+"'");
                    int notfound = 0;
                    try {
                        defidProcess = Runtime.getRuntime().exec("/usr/bin/x-terminal-emulator -e " + pathlinux + SettingsController.getInstance().DEFI_PORTFOLIO_HOME);
                    } catch (Exception e) {
                        notfound++;
                    }
                    try {
                        defidProcess = Runtime.getRuntime().exec("/usr/bin/konsole -e " + pathlinux + SettingsController.getInstance().DEFI_PORTFOLIO_HOME);
                    } catch (Exception e) {
                        notfound++;
                    }
                    if (notfound == 2) {
                        JOptionPane.showMessageDialog(null, "Could not found /usr/bin/x-terminal-emulator or\n /usr/bin/konsole", "Terminal not found", JOptionPane.ERROR_MESSAGE);
                    }
                    break;
            }


        } catch (Exception e) {
            this.settingsController.logger.warning("Exception occured: " + e.toString());
        }



    }

    public void startTimer() {
        this.settingsController.timer.scheduleAtFixedRate(new TimerController(this), 0, 15000);
    }

    public void copySelectedRawDataToClipboard(List<TransactionModel> list, boolean withHeaders) {
        StringBuilder sb = new StringBuilder();
        Locale localeDecimal = Locale.GERMAN;
        if (settingsController.selectedDecimal.getValue().equals(".")) {
            localeDecimal = Locale.US;
        }

        if (withHeaders) {
            for (TableColumn column : this.mainView.rawDataTable.getColumns()
            ) {
                sb.append(column.getId()).append(this.settingsController.selectedSeperator.getValue());
            }
            sb.setLength(sb.length() - 1);
            sb.append("\n");
        }
        for (TransactionModel transaction : list) {
            sb.append(this.transactionController.convertTimeStampToString(transaction.blockTimeProperty.getValue())).append(this.settingsController.selectedSeperator.getValue());
            sb.append(transaction.typeProperty.getValue()).append(this.settingsController.selectedSeperator.getValue());
            String[] CoinsAndAmounts = this.transactionController.splitCoinsAndAmounts(transaction.amountProperty.getValue());
            sb.append(String.format(localeDecimal, "%.8f", Double.parseDouble(CoinsAndAmounts[0]))).append(this.settingsController.selectedSeperator.getValue());
            sb.append(CoinsAndAmounts[1]).append(this.settingsController.selectedSeperator.getValue());
            sb.append(String.format(localeDecimal, "%.8f", transaction.fiatValueProperty.getValue())).append(this.settingsController.selectedSeperator.getValue());
            sb.append(this.settingsController.selectedFiatCurrency.getValue()).append(this.settingsController.selectedSeperator.getValue());
            sb.append(transaction.poolIDProperty.getValue()).append(this.settingsController.selectedSeperator.getValue());
            sb.append(transaction.blockHeightProperty.getValue()).append(this.settingsController.selectedSeperator.getValue());
            sb.append(transaction.blockHashProperty.getValue()).append(this.settingsController.selectedSeperator.getValue());
            sb.append(transaction.ownerProperty.getValue()).append(this.settingsController.selectedSeperator.getValue());
            sb.append(transaction.txIDProperty.getValue());
            sb.append("\n");
        }
        StringSelection stringSelection = new StringSelection(sb.toString());
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }

    public void copySelectedDataToClipboard(List<PoolPairModel> list, boolean withHeaders) {
        StringBuilder sb = new StringBuilder();
        Locale localeDecimal = Locale.GERMAN;
        if (settingsController.selectedDecimal.getValue().equals(".")) {
            localeDecimal = Locale.US;
        }

        if (withHeaders) {
            switch (this.mainView.tabPane.getSelectionModel().getSelectedItem().getId()) {
                case "Portfolio":
                    sb.append((this.mainView.plotTable.getColumns().get(0).getId() + "," + this.mainView.plotTable.getColumns().get(2).getId() + "," + this.mainView.plotTable.getColumns().get(9).getId()).replace(",", this.settingsController.selectedSeperator.getValue())).append("\n");
                    break;
                case "Overview":
                    sb.append((this.mainView.plotTable.getColumns().get(0).getId() + "," + this.mainView.plotTable.getColumns().get(1).getId() + "," + this.mainView.plotTable.getColumns().get(2).getId() + "," + this.mainView.plotTable.getColumns().get(3).getId() + "," + this.mainView.plotTable.getColumns().get(4).getId() + "," + this.mainView.plotTable.getColumns().get(5).getId() + "," + this.mainView.plotTable.getColumns().get(6).getId() + "," + this.mainView.plotTable.getColumns().get(7).getId() + "," + this.mainView.plotTable.getColumns().get(8).getId()).replace(",", this.settingsController.selectedSeperator.getValue())).append("\n");
                    break;
                case "Commissions":
                    sb.append((this.mainView.plotTable.getColumns().get(0).getId() + "," + this.mainView.plotTable.getColumns().get(1).getId() + "," + this.mainView.plotTable.getColumns().get(2).getId() + "," + this.mainView.plotTable.getColumns().get(3).getId() + "," + this.mainView.plotTable.getColumns().get(4).getId() + "," + this.mainView.plotTable.getColumns().get(5).getId() + "," + this.mainView.plotTable.getColumns().get(8).getId()).replace(",", this.settingsController.selectedSeperator.getValue())).append("\n");
                    break;
                case "Rewards":
                    sb.append((this.mainView.plotTable.getColumns().get(0).getId() + "," + this.mainView.plotTable.getColumns().get(1).getId() + "," + this.mainView.plotTable.getColumns().get(2).getId() + "," + this.mainView.plotTable.getColumns().get(3).getId()).replace(",", this.settingsController.selectedSeperator.getValue())).append("\n");
                    break;
                default:
                    break;
            }
        }

        for (PoolPairModel poolPair : list
        ) {
            switch (this.mainView.tabPane.getSelectionModel().getSelectedItem().getId()) {
                case "Portfolio":
                    sb.append(poolPair.getBlockTime().getValue()).append(this.settingsController.selectedSeperator.getValue());
                    sb.append(poolPair.getPoolPair().getValue()).append(this.settingsController.selectedSeperator.getValue());
                    sb.append(poolPair.getBalanceFiat().getValue()).append(this.settingsController.selectedSeperator.getValue());
                    sb.append("\n");
                    break;
                case "Overview":
                    sb.append(poolPair.getBlockTime().getValue()).append(this.settingsController.selectedSeperator.getValue());
                    sb.append(poolPair.getPoolPair().getValue()).append(this.settingsController.selectedSeperator.getValue());
                    sb.append(String.format(localeDecimal, "%.8f", poolPair.getCryptoValue1().getValue())).append(this.settingsController.selectedSeperator.getValue());
                    sb.append(String.format(localeDecimal, "%.8f", poolPair.getCryptoFiatValue1().getValue())).append(this.settingsController.selectedSeperator.getValue());
                    sb.append(String.format(localeDecimal, "%.8f", poolPair.getCryptoValue2().getValue())).append(this.settingsController.selectedSeperator.getValue());
                    sb.append(String.format(localeDecimal, "%.8f", poolPair.getCryptoFiatValue2().getValue())).append(this.settingsController.selectedSeperator.getValue());
                    sb.append(String.format(localeDecimal, "%.8f", poolPair.getcryptoCommission2Overviewvalue())).append(this.settingsController.selectedSeperator.getValue());
                    sb.append(String.format(localeDecimal, "%.8f", poolPair.getcryptoCommission2FiatOverviewvalue())).append(this.settingsController.selectedSeperator.getValue());
                    sb.append(String.format(localeDecimal, "%.8f", poolPair.getFiatValue().getValue())).append(this.settingsController.selectedSeperator.getValue());
                    sb.append("\n");
                    break;
                case "Rewards":
                case "Belohnungen":
                    sb.append(poolPair.getBlockTime().getValue()).append(this.settingsController.selectedSeperator.getValue());
                    sb.append(poolPair.getPoolPair().getValue()).append(this.settingsController.selectedSeperator.getValue());
                    sb.append(String.format(localeDecimal, "%.8f", poolPair.getCryptoValue1().getValue())).append(this.settingsController.selectedSeperator.getValue());
                    sb.append(String.format(localeDecimal, "%.8f", poolPair.getCryptoFiatValue1().getValue())).append(this.settingsController.selectedSeperator.getValue());
                    sb.append("\n");
                    break;
                case "Commissions":
                case "Kommissionen":
                    sb.append(poolPair.getBlockTime().getValue()).append(this.settingsController.selectedSeperator.getValue());
                    sb.append(poolPair.getPoolPair().getValue()).append(this.settingsController.selectedSeperator.getValue());
                    sb.append(String.format(localeDecimal, "%.8f", poolPair.getCryptoValue1().getValue())).append(this.settingsController.selectedSeperator.getValue());
                    sb.append(String.format(localeDecimal, "%.8f", poolPair.getCryptoFiatValue1().getValue())).append(this.settingsController.selectedSeperator.getValue());
                    sb.append(String.format(localeDecimal, "%.8f", poolPair.getCryptoValue2().getValue())).append(this.settingsController.selectedSeperator.getValue());
                    sb.append(String.format(localeDecimal, "%.8f", poolPair.getCryptoFiatValue2().getValue())).append(this.settingsController.selectedSeperator.getValue());
                    sb.append(String.format(localeDecimal, "%.8f", poolPair.getFiatValue().getValue())).append(this.settingsController.selectedSeperator.getValue());
                    sb.append("\n");
                    break;
                default:
                    break;
            }
        }
        StringSelection stringSelection = new StringSelection(sb.toString());
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }

    public boolean updateTransactionData() {
        TransactionController.getInstance().clearTransactionList();
        TransactionController.getInstance().clearPortfolioList();
        //this.transactionController.updateBalanceList();


        try {
            FileWriter myWriter = new FileWriter(SettingsController.getInstance().DEFI_PORTFOLIO_HOME +"update.portfolio");
            myWriter.write("<html><body>"+SettingsController.getInstance().translationList.getValue().get("UpdateData").toString()+" </body></html>");
            myWriter.close();
        } catch (IOException e) {
            SettingsController.getInstance().logger.warning("Could not write to update.portfolio."); }
        //Start Python update
        try {
            // Start skript
            switch (this.settingsController.getPlatform()) {
                case "mac":
                    // String pathMac = System.getProperty("user.dir")+"/src/portfolio/libraries/updatePortfolio ";
                    // Runtime.getRuntime().exec("/usr/bin/open -a Terminal " + pathMac + SettingsController.getInstance().DEFI_PORTFOLIO_HOME +" "+ SettingsController.getInstance().PORTFOLIO_CONFIG_FILE_PATH);
                    Runtime.getRuntime().exec("/usr/bin/open -a Terminal " + SettingsController.getInstance().DEFI_PORTFOLIO_HOME + "updatePortfolio");
                    break;
                case "win":
                    String path = System.getProperty("user.dir")+"\\defi-portfolio\\src\\portfolio\\libraries\\updatePortfolio.exe";
                    String[] commands = {"cmd", "/c", "start", "\"Update Portfolio\"", path,SettingsController.getInstance().DEFI_PORTFOLIO_HOME,SettingsController.getInstance().PORTFOLIO_CONFIG_FILE_PATH};
                    defidProcess = Runtime.getRuntime().exec(commands);
                    break;
                case "linux":
                    String pathLinux = System.getProperty("user.dir")+"/defi-portfolio/src/portfolio/libraries/updatePortfolio ";
                    SettingsController.getInstance().logger.warning(pathLinux + SettingsController.getInstance().DEFI_PORTFOLIO_HOME +" "+ SettingsController.getInstance().PORTFOLIO_CONFIG_FILE_PATH);

                    int notfound = 0;
                    try {
                        defidProcess = Runtime.getRuntime().exec("/usr/bin/x-terminal-emulator -e " + pathLinux + SettingsController.getInstance().DEFI_PORTFOLIO_HOME +" "+ SettingsController.getInstance().PORTFOLIO_CONFIG_FILE_PATH);
                    } catch (Exception e) {
                        notfound++;
                    }
                    try {
                        defidProcess = Runtime.getRuntime().exec("/usr/bin/konsole -e " + pathLinux + SettingsController.getInstance().DEFI_PORTFOLIO_HOME +" "+ SettingsController.getInstance().PORTFOLIO_CONFIG_FILE_PATH);
                    } catch (Exception e) {
                        notfound++;
                    }
                    if (notfound == 2) {
                        JOptionPane.showMessageDialog(null, "Could not found /usr/bin/x-terminal-emulator or\n /usr/bin/konsole", "Terminal not found", JOptionPane.ERROR_MESSAGE);
                    }
                    break;
            }


        } catch (Exception e) {
            this.settingsController.logger.warning("Exception occured: " + e.toString());
        }

        try {
            File f = new File(SettingsController.getInstance().DEFI_PORTFOLIO_HOME + "pythonUpdate.portfolio");
            f.createNewFile();

        } catch (Exception e) {
            SettingsController.getInstance().logger.warning("Could not write python update file."); }

       /* if (new File(this.settingsController.DEFI_PORTFOLIO_HOME + this.settingsController.strTransactionData).exists()) {
            int depth = Integer.parseInt(this.transactionController.getBlockCount()) - this.transactionController.getLocalBlockCount();
            return transactionController.updateTransactionData(depth);
        } else {
            return transactionController.updateTransactionData(Integer.parseInt(transactionController.getBlockCount()));
        }
*/
        return true;

    }

    public void finishedUpdate(){
        try {
            FileWriter myWriter = new FileWriter(SettingsController.getInstance().DEFI_PORTFOLIO_HOME + "update.portfolio");
            myWriter.write("<html><body>"+SettingsController.getInstance().translationList.getValue().get("PreparingData").toString()+"</body></html>");
            myWriter.close();
        } catch (IOException e) {
            SettingsController.getInstance().logger.warning("Could not write to update.portfolio."); }

        this.transactionController.updateTransactionList(this.transactionController.getLocalTransactionList());
        int localBlockCount = this.transactionController.getLocalBlockCount();
        int blockCount = Integer.parseInt(this.transactionController.getBlockCount());
        this.strCurrentBlockLocally.set(Integer.toString(localBlockCount));
        if (localBlockCount > blockCount) {
            this.strCurrentBlockOnBlockchain.set(Integer.toString(localBlockCount));
        } else {
            this.strCurrentBlockOnBlockchain.set(Integer.toString(blockCount));
        }
        this.transactionController.calcImpermanentLoss();
        Date date = new Date(System.currentTimeMillis());
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        this.settingsController.lastUpdate.setValue(dateFormat.format(date));
        this.settingsController.saveSettings();
        this.bDataBase.setValue(this.updateSingleton = true);
        this.plotUpdate(this.mainView.tabPane.getSelectionModel().getSelectedItem().getId());
        File file = new File(SettingsController.getInstance().DEFI_PORTFOLIO_HOME +  "update.portfolio");
        if (file.exists()) file.delete();
        TransactionController.getInstance().ps.destroy();
    }

    public void btnUpdateDatabasePressed() {
        if (this.updateSingleton) {
            this.bDataBase.setValue(this.updateSingleton = false);
            updateTransactionData();
        }

    }

    public void plotUpdate(String openedTab) {
        switch (openedTab) {
            case "Portfolio":
                updatePortfolio();
                break;
            case "Overview":
            case "Übersicht":
                updateOverview();
                break;
            case "Rewards":
            case "Belohnungen":
                updateRewards();
                break;
            case "Commissions":
            case "Kommissionen":
                updateCommissions();
                break;
            default:
                break;
        }
    }

    private String getColor(String tokenName) {
        switch (tokenName) {
            case "DFI":
                tokenName = "#FF00AF";
                break;
            case "ETH-DFI":
            case "ETH":
                tokenName = "#14044d";
                break;
            case "BTC":
            case "BTC-DFI":
                tokenName = "#f7931a";
                break;
            case "USDT":
            case "USDT-DFI":
                tokenName = "#0ecc8d";
                break;
            case "DOGE":
            case "DOGE-DFI":
                tokenName = "#cb9800";
                break;
            case "LTC":
            case "LTC-DFI":
                tokenName = "#00aeff";
                break;
            case "BCH":
            case "BCH-DFI":
                tokenName = "#478559";
                break;
            case "USDC":
            case "USDC-DFI":
                tokenName = "#2775CA";
                break;
            default:
                tokenName = "-";
                break;
        }
        return tokenName;
    }

    private void updatePortfolio() {


        this.poolPairModelList.clear();
        this.poolPairList.clear();

        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        ObservableList<PieChart.Data> pieChartData2 = FXCollections.observableArrayList();
        String currency = "\u20ac";
        if (SettingsController.getInstance().selectedFiatCurrency.getValue().equals("USD")) {
            currency = "\u0024";
        } else if (SettingsController.getInstance().selectedFiatCurrency.getValue().equals("CHF")) {
            currency = "CHF";
        }
        Double calculatedPortfolio = 0.0;
        Double calculatedPortfolio2 = 0.0;
        Locale localeDecimal = Locale.GERMAN;
        if (this.settingsController.selectedDecimal.getValue().equals(".")) {
            localeDecimal = Locale.US;
        }
        for (BalanceModel balanceModel : this.transactionController.getBalanceList()) {

            if (balanceModel.getToken2NameValue().equals("-")) {
                pieChartData.add(new PieChart.Data(balanceModel.getToken1NameValue(), balanceModel.getFiat1Value()));
                this.poolPairModelList.add(new PoolPairModel(balanceModel.getToken1NameValue() + " (" + String.format(localeDecimal, "%1.2f", CoinPriceController.getInstance().getPriceFromTimeStamp(balanceModel.getToken2NameValue().equals("DUSD"),balanceModel.getToken1NameValue() + this.settingsController.selectedFiatCurrency.getValue(), System.currentTimeMillis())) + currency + ")", 0.0, 0.0, 0.0, String.format(localeDecimal, "%1.8f", balanceModel.getCrypto1Value()), 0.0, 0.0, 0.0, 0.0, String.format(localeDecimal, "%,.2f", balanceModel.getFiat1Value())));
                calculatedPortfolio += balanceModel.getFiat1Value() + balanceModel.getFiat2Value();

            } else {
                pieChartData2.add(new PieChart.Data(balanceModel.getToken1NameValue() + "-" + balanceModel.getToken2NameValue(), balanceModel.getFiat1Value() + balanceModel.getFiat2Value()));
                this.poolPairModelList.add(new PoolPairModel(balanceModel.getToken1NameValue() + "-" + balanceModel.getToken2NameValue() + " (" + String.format(localeDecimal, "%1.2f", (balanceModel.getFiat1Value() + balanceModel.getFiat2Value()) / balanceModel.getShareValue()) + currency + ")", 0.0, 0.0, 0.0,
                        String.format(localeDecimal, "%1.8f", balanceModel.getShareValue()) + " (" + String.format(localeDecimal, "%1.8f", balanceModel.getCrypto1Value()) + " " + balanceModel.getToken1NameValue() + " + " + String.format(localeDecimal, "%1.8f", balanceModel.getCrypto2Value()) + balanceModel.getToken2NameValue() + ")",
                        0.0, 0.0, 0.0, 0.0, String.format(localeDecimal, "%,.2f", balanceModel.getFiat1Value() + balanceModel.getFiat1Value()) + " (" + String.format(localeDecimal, "%,.2f", balanceModel.getFiat1Value()) + " " + balanceModel.getToken1NameValue() + " + " + String.format(localeDecimal, "%,.2f", balanceModel.getFiat2Value()) + balanceModel.getToken2NameValue() + ")"));
                calculatedPortfolio2 += balanceModel.getFiat1Value() + balanceModel.getFiat2Value();
            }

        }

        double totalYield = 0;
        double totalYieldRewards = 0;
        double totalYieldCommissions = 0;

        for (String poolPair : this.settingsController.cryptoCurrencies) {

            double poolPair1Price = CoinPriceController.getInstance().getPriceFromTimeStamp(poolPair.contains("DUSD"),poolPair.split("-")[1] + this.settingsController.selectedFiatCurrency.getValue(), System.currentTimeMillis());
            double poolPair2Price = CoinPriceController.getInstance().getPriceFromTimeStamp(poolPair.contains("DUSD"),poolPair.split("-")[0] + this.settingsController.selectedFiatCurrency.getValue(), System.currentTimeMillis());

            if (this.transactionController.getPortfolioList().containsKey(poolPair + "-" + this.settingsController.selectedIntervallInt)) {
                for (HashMap.Entry<String, PortfolioModel> entry : this.transactionController.getPortfolioList().get(poolPair + "-" + this.settingsController.selectedIntervallInt).entrySet()) {
                    totalYield += (entry.getValue().getCoinCommissions1Value() * poolPair1Price) + (entry.getValue().getCoinCommissions2Value() * poolPair2Price) + (entry.getValue().getCoinRewards1Value() * poolPair1Price);
                    totalYieldRewards += entry.getValue().getCoinRewards1Value() * poolPair1Price;
                    totalYieldCommissions += (entry.getValue().getCoinCommissions1Value() * poolPair1Price) + (entry.getValue().getCoinCommissions2Value() * poolPair2Price);
                }
            }
        }

        this.settingsController.tokenYield.set(this.settingsController.translationList.getValue().get("TotalYield") + ":\n" + String.format(localeDecimal, "%,.2f", totalYield) + currency);
        this.settingsController.tokenYieldRewards.set(this.settingsController.translationList.getValue().get("TotalYieldRewards") + ":\n" + String.format(localeDecimal, "%,.2f", totalYieldRewards) + currency);
        this.settingsController.tokenYieldCommissions.set(this.settingsController.translationList.getValue().get("TotalYieldCommissions") + ":\n" + String.format(localeDecimal, "%,.2f", totalYieldCommissions) + currency);
        this.settingsController.tokenAmount.set(this.settingsController.translationList.getValue().get("TotalAmount") + ":\n" + String.format(localeDecimal, "%,.2f", calculatedPortfolio + calculatedPortfolio2) + currency);
        this.settingsController.tokenBalance.set("Token:\n" + String.format(localeDecimal, "%,.2f", calculatedPortfolio) + currency);
        this.settingsController.tokenBalanceLM.set("LM Token:\n" + String.format(localeDecimal, "%,.2f", calculatedPortfolio2) + currency);
        this.mainView.plotPortfolio1.setData(pieChartData);
        this.mainView.plotPortfolio11.setData(pieChartData2);


        for (PieChart.Data data : this.mainView.plotPortfolio1.getData()
        ) {
            if(!getColor(data.getName()).equals("-"))  data.getNode().setStyle("-fx-pie-color: " + getColor(data.getName()) + ";");
        }

        for (Node n : this.mainView.plotPortfolio1.getChildrenUnmodifiable()
        ) {
            if (n instanceof Legend) {
                for (Legend.LegendItem legendItem : ((Legend) n).getItems()) {
                    if(!getColor(legendItem.getText()).equals("-")) legendItem.getSymbol().setStyle("-fx-background-color: " + getColor(legendItem.getText()) + ";");
                }
            }
        }

        for (PieChart.Data data : this.mainView.plotPortfolio11.getData()
        ) {
           if(!getColor(data.getName()).equals("-")) data.getNode().setStyle("-fx-pie-color: " + getColor(data.getName()) + ";");
        }

        for (Node n : this.mainView.plotPortfolio11.getChildrenUnmodifiable()
        ) {
            if (n instanceof Legend) {
                for (Legend.LegendItem legendItem : ((Legend) n).getItems()) {
                  if(!getColor(legendItem.getText()).equals("-"))  legendItem.getSymbol().setStyle("-fx-background-color: " + getColor(legendItem.getText()) + ";");
                }
            }
        }

        this.mainView.plotPortfolio1.getData().forEach(data -> {
            Tooltip toolTip = new Tooltip(String.format("%1.2f", data.getPieValue()) + " " + SettingsController.getInstance().selectedFiatCurrency.getValue());
            Tooltip.install(data.getNode(), toolTip);
        });

        this.mainView.plotPortfolio11.getData().forEach(data -> {
            Tooltip toolTip = new Tooltip(String.format("%1.2f", data.getPieValue()) + " " + SettingsController.getInstance().selectedFiatCurrency.getValue());
            Tooltip.install(data.getNode(), toolTip);
        });

        this.poolPairModelList.sort(Comparator.comparing(PoolPairModel::getBlockTimeValue));

        this.poolPairList.clear();

        if (poolPairModelList.size() > 0 & TransactionController.getInstance().impermanentLossList.size()>0) {

            // add Impermanent Loss
            this.poolPairModelList.add(new PoolPairModel("", 0.0, 0.0, 0.0, "", 0.0, 0.0, 0.0, 0.0, ""));
            this.poolPairModelList.add(new PoolPairModel("Impermanent Loss", 0.0, 0.0, 0.0, "Value input coins" + " (" + SettingsController.getInstance().selectedFiatCurrency.getValue() + ")", 0.0, 0.0, 0.0, 0.0, "Value current coins" + " (" + SettingsController.getInstance().selectedFiatCurrency.getValue() + ")"));

            double currentDFIPrice = CoinPriceController.getInstance().getPriceFromTimeStamp(false,"DFI" + this.settingsController.selectedFiatCurrency.getValue(), System.currentTimeMillis());
            double currentDUSDPrice = CoinPriceController.getInstance().getPriceFromTimeStamp(true,"DUSD"+ this.settingsController.selectedFiatCurrency.getValue(),System.currentTimeMillis());
            double currentCoin1Price;
            TreeMap<String, ImpermanentLossModel> ilList = TransactionController.getInstance().impermanentLossList;
            double inputTotal = 0.0;
            double currentTotal = 0.0;
            for (String key : ilList.keySet()) {
                double currentCoin2Price = CoinPriceController.getInstance().getPriceFromTimeStamp(key.contains("DUSD"),key.split("-")[0] + this.settingsController.selectedFiatCurrency.getValue(), System.currentTimeMillis());

                if(key.split("-")[1].equals("DFI")) {
                    currentCoin1Price = currentDFIPrice;
                }else{
                    currentCoin1Price = currentDUSDPrice;
                }

                double valueInputCoins = currentCoin1Price * ilList.get(key).PoolCoin1 + currentCoin2Price * ilList.get(key).PoolCoin2;
                double valuePool = 0.0;
                for (BalanceModel balanceModel : this.transactionController.getBalanceList()) {
                    if (!balanceModel.getToken2NameValue().equals("-")) {
                        if (key.split("-")[0].equals(balanceModel.getToken1Name().getValue())) {
                            valuePool = balanceModel.getFiat1Value() + balanceModel.getFiat2Value();
                        }
                    }
                }

                String lossValueString;
                String valuePoolString;

                if(valuePool == 0){
                    lossValueString = "-";
                    valuePoolString = "-";
                }else{
                    double lossValue = ((valuePool / valueInputCoins) - 1) * 100;
                    if(lossValue > 0) {
                        lossValue = lossValue * -1;
                    }
                    inputTotal += valueInputCoins;
                    currentTotal += valuePool;

                    lossValueString=  String.format(localeDecimal, "%,.2f", lossValue) + "%";
                    valuePoolString= String.format(localeDecimal, "%,1.2f", valuePool);
                }
                this.poolPairModelList.add(new PoolPairModel(lossValueString + " (" + key + ")", 0.0, 0.0, 0.0, String.format(localeDecimal, "%,1.2f", valueInputCoins), 0.0, 0.0, 0.0, 0.0, valuePoolString));
            }

            this.poolPairModelList.add(new PoolPairModel(String.format(localeDecimal, "%,.2f", ((currentTotal / inputTotal) - 1) * 100) + "%" + " (Total)", 0.0, 0.0, 0.0, String.format(localeDecimal, "%,1.2f", inputTotal), 0.0, 0.0, 0.0, 0.0, String.format(localeDecimal, "%,1.2f", currentTotal)));
            this.poolPairList.addAll(this.poolPairModelList);

        }
    }


    public void updateOverview() {
        try {

            this.poolPairModelList.clear();
            this.mainView.plotOverview.setLegendVisible(true);
            this.mainView.plotOverview.getData().clear();
            this.mainView.plotOverview.getYAxis().setLabel("Total (" + this.settingsController.selectedFiatCurrency.getValue() + ")");
            double maxValue = 0;

            for (String poolPair : this.settingsController.cryptoCurrencies) {

                XYChart.Series<Number, Number> overviewSeries = new XYChart.Series();
                overviewSeries.setName(poolPair);

                if (this.transactionController.getPortfolioList().containsKey(poolPair + "-" + this.settingsController.selectedIntervallInt)) {

                    for (HashMap.Entry<String, PortfolioModel> entry : this.transactionController.getPortfolioList().get(poolPair + "-" + this.settingsController.selectedIntervallInt).entrySet()) {
                        if (entry.getValue().getDateValue().compareTo(this.transactionController.convertDateToIntervall(this.settingsController.dateFrom.getValue().toString(), this.settingsController.selectedIntervallInt)) >= 0 &&
                                entry.getValue().getDateValue().compareTo(this.transactionController.convertDateToIntervall(this.settingsController.dateTo.getValue().toString(), this.settingsController.selectedIntervallInt)) <= 0) {

                            if (poolPair.equals(entry.getValue().getPoolPairValue())) {
                                overviewSeries.getData().add(new XYChart.Data(entry.getKey(), entry.getValue().getFiatRewards1Value() + entry.getValue().getFiatCommissions1Value() + entry.getValue().getFiatCommissions2Value()));
                                this.poolPairModelList.add(new PoolPairModel(entry.getKey(), entry.getValue().getFiatRewards1Value() + entry.getValue().getFiatCommissions1Value() + entry.getValue().getFiatCommissions2Value(), entry.getValue().getCoinRewards().getValue(), entry.getValue().getCoinCommissions1Value(), poolPair, entry.getValue().getFiatRewards1Value(), entry.getValue().getFiatCommissions1Value(), entry.getValue().getCoinCommissions2Value(), entry.getValue().getFiatCommissions2Value(), ""));
                            }
                        }
                    }

                    this.mainView.yAxis.setAutoRanging(false);

                    if (overviewSeries.getData().size() > 0) {
                        maxValue += overviewSeries.getData().stream().mapToDouble(d -> (Double) d.getYValue()).max().getAsDouble();
                        this.mainView.yAxis.setUpperBound(maxValue * 1.1);
                        this.mainView.plotOverview.getData().add(overviewSeries);
                        this.mainView.plotOverview.setCreateSymbols(true);
                    }
                }

            }
            for (XYChart.Series<Number, Number> s : this.mainView.plotOverview.getData()) {
                if (s != null) {
                    for (XYChart.Data d : s.getData()) {
                        if (d != null) {
                            Tooltip t = new Tooltip(d.getYValue().toString());
                            Tooltip.install(d.getNode(), t);
                            d.getNode().setOnMouseEntered(event -> d.getNode().getStyleClass().add("onHover"));
                            d.getNode().setOnMouseExited(event -> d.getNode().getStyleClass().remove("onHover"));
                        }
                    }
                }
            }

            this.poolPairModelList.sort(Comparator.comparing(PoolPairModel::getBlockTimeValue));
            this.poolPairList.clear();
            this.poolPairList.addAll(this.poolPairModelList);

        } catch (Exception e) {
            this.settingsController.logger.warning(e.toString());
        }
    }

    public void updateRewards() {

        XYChart.Series<Number, Number> rewardsSeries = new XYChart.Series();
        this.poolPairModelList.clear();
        this.mainView.plotRewards.setLegendVisible(false);
        this.mainView.plotRewards.getData().clear();

        if (this.settingsController.selectedPlotCurrency.getValue().equals("Coin")) {
            this.mainView.plotRewards.getYAxis().setLabel(this.settingsController.selectedCoin.getValue().split("-")[1]);
        } else if (this.settingsController.selectedPlotCurrency.getValue().equals("Daily Fiat") || this.settingsController.selectedPlotCurrency.getValue().equals("Current Fiat")) {
            this.mainView.plotRewards.getYAxis().setLabel(this.settingsController.selectedCoin.getValue().split("-")[1] + " (" + this.settingsController.selectedFiatCurrency.getValue() + ")");
        }

        if (this.transactionController.getPortfolioList().containsKey(this.settingsController.selectedCoin.getValue() + "-" + this.settingsController.selectedIntervallInt)) {

            if (this.settingsController.selectedPlotType.getValue().equals(this.settingsController.translationList.getValue().get("Individual"))) {

                for (HashMap.Entry<String, PortfolioModel> entry : this.transactionController.getPortfolioList().get(this.settingsController.selectedCoin.getValue() + "-" + this.settingsController.selectedIntervallInt).entrySet()) {

                    if (entry.getValue().getDateValue().compareTo(this.transactionController.convertDateToIntervall(this.settingsController.dateFrom.getValue().toString(), this.settingsController.selectedIntervallInt)) >= 0 &&
                            entry.getValue().getDateValue().compareTo(this.transactionController.convertDateToIntervall(this.settingsController.dateTo.getValue().toString(), this.settingsController.selectedIntervallInt)) <= 0) {

                        if (this.settingsController.selectedPlotCurrency.getValue().equals("Coin")) {
                            rewardsSeries.getData().add(new XYChart.Data(entry.getKey(), entry.getValue().getCoinRewards1Value()));
                        } else if (this.settingsController.selectedPlotCurrency.getValue().equals("Daily Fiat")) {
                            rewardsSeries.getData().add(new XYChart.Data(entry.getKey(), entry.getValue().getFiatRewards1Value()));
                        } else if (this.settingsController.selectedPlotCurrency.getValue().equals("Current Fiat")) {
                            double currentDFIPrice = CoinPriceController.getInstance().getPriceFromTimeStamp(false,"DFI" + this.settingsController.selectedFiatCurrency.getValue(), System.currentTimeMillis());
                            rewardsSeries.getData().add(new XYChart.Data(entry.getKey(), entry.getValue().getCoinRewards1Value() * currentDFIPrice));
                        }

                        if (this.settingsController.selectedPlotCurrency.getValue().equals("Current Fiat")) {
                            double currentDFIPrice = CoinPriceController.getInstance().getPriceFromTimeStamp(false,"DFI" + this.settingsController.selectedFiatCurrency.getValue(), System.currentTimeMillis());
                            this.poolPairModelList.add(new PoolPairModel(entry.getKey(), 1, entry.getValue().getCoinRewards1Value(), 1, this.settingsController.selectedCoin.getValue(), entry.getValue().getCoinRewards1Value() * currentDFIPrice, 1, 1.0, 1, ""));
                        } else {
                            this.poolPairModelList.add(new PoolPairModel(entry.getKey(), 1, entry.getValue().getCoinRewards1Value(), 1, this.settingsController.selectedCoin.getValue(), entry.getValue().getFiatRewards1Value(), 1, 1.0, 1, ""));
                        }
                    }
                }


                if (this.mainView.plotRewards.getData().size() == 1) {
                    this.mainView.plotRewards.getData().remove(0);
                }

                this.mainView.plotRewards.getData().add(rewardsSeries);

                for (XYChart.Series<Number, Number> s : this.mainView.plotRewards.getData()) {
                    for (XYChart.Data d : s.getData()) {
                        Tooltip t = new Tooltip(d.getYValue().toString());
                        Tooltip.install(d.getNode(), t);
                        d.getNode().setOnMouseEntered(event -> d.getNode().getStyleClass().add("onHover"));
                        d.getNode().setOnMouseExited(event -> d.getNode().getStyleClass().remove("onHover"));
                    }
                }

            } else {

                XYChart.Series<Number, Number> rewardsCumulated = new XYChart.Series();

                double cumulatedCoinValue = 0;
                double cumulatedFiatValue = 0;

                for (HashMap.Entry<String, PortfolioModel> entry : this.transactionController.getPortfolioList().get(this.settingsController.selectedCoin.getValue() + "-" + this.settingsController.selectedIntervallInt).entrySet()) {
                    if (entry.getValue().getDateValue().compareTo(this.transactionController.convertDateToIntervall(this.settingsController.dateFrom.getValue().toString(), this.settingsController.selectedIntervallInt)) >= 0 &&
                            entry.getValue().getDateValue().compareTo(this.transactionController.convertDateToIntervall(this.settingsController.dateTo.getValue().toString(), this.settingsController.selectedIntervallInt)) <= 0) {

                        if (this.settingsController.selectedPlotCurrency.getValue().equals("Coin")) {
                            cumulatedCoinValue = cumulatedCoinValue + entry.getValue().getCoinRewards1Value();
                            rewardsCumulated.getData().add(new XYChart.Data(entry.getKey(), cumulatedCoinValue));
                        } else if (this.settingsController.selectedPlotCurrency.getValue().equals("Daily Fiat")) {
                            cumulatedFiatValue = cumulatedFiatValue + entry.getValue().getFiatRewards1Value();
                            rewardsCumulated.getData().add(new XYChart.Data(entry.getKey(), cumulatedFiatValue));
                        } else if (this.settingsController.selectedPlotCurrency.getValue().equals("Current Fiat")) {
                            double currentDFIPrice = CoinPriceController.getInstance().getPriceFromTimeStamp(false,"DFI" + this.settingsController.selectedFiatCurrency.getValue(), System.currentTimeMillis());
                            cumulatedFiatValue = cumulatedFiatValue + (entry.getValue().getCoinRewards1Value() * currentDFIPrice);
                            rewardsCumulated.getData().add(new XYChart.Data(entry.getKey(), cumulatedFiatValue));
                        }
                        if (this.settingsController.selectedPlotCurrency.getValue().equals("Current Fiat")) {
                            double currentDFIPrice = CoinPriceController.getInstance().getPriceFromTimeStamp(false,"DFI" + this.settingsController.selectedFiatCurrency.getValue(), System.currentTimeMillis());
                            this.poolPairModelList.add(new PoolPairModel(entry.getKey(), 1, entry.getValue().getCoinRewards1Value(), 1, this.settingsController.selectedCoin.getValue(), entry.getValue().getCoinRewards1Value() * currentDFIPrice, 1, 1.0, 1, ""));
                        } else {
                            this.poolPairModelList.add(new PoolPairModel(entry.getKey(), 1, entry.getValue().getCoinRewards1Value(), 1, this.settingsController.selectedCoin.getValue(), entry.getValue().getFiatRewards1Value(), 1, 1.0, 1, ""));
                        }
                    }
                }
                if (this.mainView.plotRewards.getData().size() == 1) {
                    this.mainView.plotRewards.getData().remove(0);
                }

                this.mainView.plotRewards.getData().add(rewardsCumulated);

                for (XYChart.Series<Number, Number> s : this.mainView.plotRewards.getData()) {
                    for (XYChart.Data d : s.getData()) {
                        Tooltip t = new Tooltip(d.getYValue().toString());
                        Tooltip.install(d.getNode(), t);
                        d.getNode().setOnMouseEntered(event -> d.getNode().getStyleClass().add("onHover"));
                        d.getNode().setOnMouseExited(event -> d.getNode().getStyleClass().remove("onHover"));
                    }
                }
            }
        }

        this.poolPairModelList.sort(Comparator.comparing(PoolPairModel::getBlockTimeValue));
        this.poolPairList.clear();
        this.poolPairList.addAll(this.poolPairModelList);
    }


    public void updateCommissions() {

        XYChart.Series<Number, Number> commissionsSeries1 = new XYChart.Series();
        XYChart.Series<Number, Number> commissionsSeries2 = new XYChart.Series();
        this.mainView.plotCommissions1.getData().clear();
        this.mainView.plotCommissions2.getData().clear();
        this.poolPairModelList.clear();
        this.poolPairList.clear();
        this.mainView.plotCommissions1.setLegendVisible(false);
        this.mainView.plotCommissions2.setLegendVisible(false);

        if (this.settingsController.selectedPlotCurrency.getValue().equals("Coin")) {
            this.mainView.plotCommissions1.getYAxis().setLabel(this.settingsController.selectedCoin.getValue().split("-")[1]);
            this.mainView.plotCommissions2.getYAxis().setLabel(this.settingsController.selectedCoin.getValue().split("-")[0]);
        } else if (this.settingsController.selectedPlotCurrency.getValue().equals("Daily Fiat") || this.settingsController.selectedPlotCurrency.getValue().equals("Current Fiat")) {
            this.mainView.plotCommissions1.getYAxis().setLabel(this.settingsController.selectedCoin.getValue().split("-")[1] + " (" + this.settingsController.selectedFiatCurrency.getValue() + ")");
            this.mainView.plotCommissions2.getYAxis().setLabel(this.settingsController.selectedCoin.getValue().split("-")[0] + " (" + this.settingsController.selectedFiatCurrency.getValue() + ")");
        }

        if (this.transactionController.getPortfolioList().containsKey(this.settingsController.selectedCoin.getValue() + "-" + this.settingsController.selectedIntervallInt)) {

            if (this.settingsController.selectedPlotType.getValue().equals(this.settingsController.translationList.getValue().get("Individual"))) {

                for (HashMap.Entry<String, PortfolioModel> entry : this.transactionController.getPortfolioList().get(this.settingsController.selectedCoin.getValue() + "-" + this.settingsController.selectedIntervallInt).entrySet()) {
                    if (entry.getValue().getDateValue().compareTo(this.transactionController.convertDateToIntervall(this.settingsController.dateFrom.getValue().toString(), this.settingsController.selectedIntervallInt)) >= 0 &&
                            entry.getValue().getDateValue().compareTo(this.transactionController.convertDateToIntervall(this.settingsController.dateTo.getValue().toString(), this.settingsController.selectedIntervallInt)) <= 0) {

                        if (this.settingsController.selectedPlotCurrency.getValue().equals("Coin")) {
                            commissionsSeries1.getData().add(new XYChart.Data(entry.getKey(), entry.getValue().getCoinCommissions1Value()));
                            commissionsSeries2.getData().add(new XYChart.Data(entry.getKey(), entry.getValue().getCoinCommissions2Value()));
                        } else if (this.settingsController.selectedPlotCurrency.getValue().equals("Daily Fiat")) {
                            commissionsSeries1.getData().add(new XYChart.Data(entry.getKey(), entry.getValue().getFiatCommissions1Value()));
                            commissionsSeries2.getData().add(new XYChart.Data(entry.getKey(), entry.getValue().getFiatCommissions2Value()));
                        } else if (this.settingsController.selectedPlotCurrency.getValue().equals("Current Fiat")) {
                            double poolPair1CurrentPrice = CoinPriceController.getInstance().getPriceFromTimeStamp(this.settingsController.selectedCoin.getValue().contains("DUSD"),this.settingsController.selectedCoin.getValue().split("-")[1] + this.settingsController.selectedFiatCurrency.getValue(), System.currentTimeMillis());
                            double poolPair2CurrentPrice = CoinPriceController.getInstance().getPriceFromTimeStamp(this.settingsController.selectedCoin.getValue().contains("DUSD"),this.settingsController.selectedCoin.getValue().split("-")[0] + this.settingsController.selectedFiatCurrency.getValue(), System.currentTimeMillis());

                            commissionsSeries1.getData().add(new XYChart.Data(entry.getKey(), entry.getValue().getCoinCommissions1Value() * poolPair1CurrentPrice));
                            commissionsSeries2.getData().add(new XYChart.Data(entry.getKey(), entry.getValue().getCoinCommissions2Value() * poolPair2CurrentPrice));
                        }

                        if (this.settingsController.selectedPlotCurrency.getValue().equals("Current Fiat")) {
                            double poolPair1CurrentPrice = CoinPriceController.getInstance().getPriceFromTimeStamp(this.settingsController.selectedCoin.getValue().split("-")[1].equals("DUSD"),this.settingsController.selectedCoin.getValue().split("-")[1] + this.settingsController.selectedFiatCurrency.getValue(), System.currentTimeMillis());
                            double poolPair2CurrentPrice = CoinPriceController.getInstance().getPriceFromTimeStamp(this.settingsController.selectedCoin.getValue().split("-")[0].equals("DUSD"),this.settingsController.selectedCoin.getValue().split("-")[0] + this.settingsController.selectedFiatCurrency.getValue(), System.currentTimeMillis());

                            this.poolPairModelList.add(new PoolPairModel(entry.getKey(), (entry.getValue().getCoinCommissions1Value() * poolPair1CurrentPrice) + (entry.getValue().getCoinCommissions2Value() * poolPair2CurrentPrice), entry.getValue().getCoinCommissions1Value(), entry.getValue().getCoinCommissions2Value(), this.settingsController.selectedCoin.getValue(), entry.getValue().getCoinCommissions1Value() * poolPair1CurrentPrice, entry.getValue().getCoinCommissions2Value() * poolPair2CurrentPrice, 1.0, 1, ""));
                        } else {
                            this.poolPairModelList.add(new PoolPairModel(entry.getKey(), entry.getValue().getFiatCommissions1Value() + entry.getValue().getFiatCommissions2Value(), entry.getValue().getCoinCommissions1Value(), entry.getValue().getCoinCommissions2Value(), this.settingsController.selectedCoin.getValue(), entry.getValue().getFiatCommissions1Value(), entry.getValue().getFiatCommissions2Value(), 1.0, 1, ""));
                        }
                    }
                }

                this.mainView.plotCommissions1.getData().add(commissionsSeries1);
                this.mainView.plotCommissions2.getData().add(commissionsSeries2);

                for (XYChart.Series<Number, Number> s : this.mainView.plotCommissions1.getData()) {
                    for (XYChart.Data d : s.getData()) {
                        Tooltip t = new Tooltip(d.getYValue().toString());
                        Tooltip.install(d.getNode(), t);
                        d.getNode().setOnMouseEntered(event -> d.getNode().getStyleClass().add("onHover"));
                        d.getNode().setOnMouseExited(event -> d.getNode().getStyleClass().remove("onHover"));
                    }
                }

                for (XYChart.Series<Number, Number> s : this.mainView.plotCommissions2.getData()) {
                    for (XYChart.Data d : s.getData()) {
                        Tooltip t = new Tooltip(d.getYValue().toString());
                        Tooltip.install(d.getNode(), t);
                        d.getNode().setOnMouseEntered(event -> d.getNode().getStyleClass().add("onHover"));
                        d.getNode().setOnMouseExited(event -> d.getNode().getStyleClass().remove("onHover"));
                    }
                }

            } else {

                XYChart.Series<Number, Number> rewardsCumulated1 = new XYChart.Series();
                XYChart.Series<Number, Number> rewardsCumulated2 = new XYChart.Series();

                double cumulatedCommissions1CoinValue = 0;
                double cumulatedCommissions1FiatValue = 0;
                double cumulatedCommissions2CoinValue = 0;
                double cumulatedCommissions2FiatValue = 0;
                for (HashMap.Entry<String, PortfolioModel> entry : this.transactionController.getPortfolioList().get(this.settingsController.selectedCoin.getValue() + "-" + this.settingsController.selectedIntervallInt).entrySet()) {
                    if (entry.getValue().getDateValue().compareTo(this.transactionController.convertDateToIntervall(this.settingsController.dateFrom.getValue().toString(), this.settingsController.selectedIntervallInt)) >= 0 &&
                            entry.getValue().getDateValue().compareTo(this.transactionController.convertDateToIntervall(this.settingsController.dateTo.getValue().toString(), this.settingsController.selectedIntervallInt)) <= 0) {
                        if (this.settingsController.selectedPlotCurrency.getValue().equals("Coin")) {
                            cumulatedCommissions1CoinValue = cumulatedCommissions1CoinValue + entry.getValue().getCoinCommissions1Value();
                            cumulatedCommissions2CoinValue = cumulatedCommissions2CoinValue + entry.getValue().getCoinCommissions2Value();
                            rewardsCumulated1.getData().add(new XYChart.Data(entry.getKey(), cumulatedCommissions1CoinValue));
                            rewardsCumulated2.getData().add(new XYChart.Data(entry.getKey(), cumulatedCommissions2CoinValue));
                        } else if (this.settingsController.selectedPlotCurrency.getValue().equals("Daily Fiat")) {
                            cumulatedCommissions1FiatValue = cumulatedCommissions1FiatValue + entry.getValue().getFiatCommissions1Value();
                            cumulatedCommissions2FiatValue = cumulatedCommissions2FiatValue + entry.getValue().getFiatCommissions2Value();
                            rewardsCumulated1.getData().add(new XYChart.Data(entry.getKey(), cumulatedCommissions1FiatValue));
                            rewardsCumulated2.getData().add(new XYChart.Data(entry.getKey(), cumulatedCommissions2FiatValue));
                        } else if (this.settingsController.selectedPlotCurrency.getValue().equals("Current Fiat")) {
                            double poolPair1CurrentPrice = CoinPriceController.getInstance().getPriceFromTimeStamp(this.settingsController.selectedCoin.getValue().contains("DUSD"),this.settingsController.selectedCoin.getValue().split("-")[1] + this.settingsController.selectedFiatCurrency.getValue(), System.currentTimeMillis());
                            double poolPair2CurrentPrice = CoinPriceController.getInstance().getPriceFromTimeStamp(this.settingsController.selectedCoin.getValue().contains("DUSD"),this.settingsController.selectedCoin.getValue().split("-")[0] + this.settingsController.selectedFiatCurrency.getValue(), System.currentTimeMillis());

                            cumulatedCommissions1FiatValue = cumulatedCommissions1FiatValue + (entry.getValue().getCoinCommissions1Value() * poolPair1CurrentPrice);
                            cumulatedCommissions2FiatValue = cumulatedCommissions2FiatValue + (entry.getValue().getCoinCommissions2Value() * poolPair2CurrentPrice);
                            rewardsCumulated1.getData().add(new XYChart.Data(entry.getKey(), cumulatedCommissions1FiatValue));
                            rewardsCumulated2.getData().add(new XYChart.Data(entry.getKey(), cumulatedCommissions2FiatValue));
                        }
                        if (this.settingsController.selectedPlotCurrency.getValue().equals("Current Fiat")) {
                            double currentDFIPrice = CoinPriceController.getInstance().getPriceFromTimeStamp(false,"DFI" + this.settingsController.selectedFiatCurrency.getValue(), System.currentTimeMillis());
                            double currentPairPrice = CoinPriceController.getInstance().getPriceFromTimeStamp(this.settingsController.selectedCoin.getValue().contains("DUSD"),this.settingsController.selectedCoin.getValue().split("-")[0] + this.settingsController.selectedFiatCurrency.getValue(), System.currentTimeMillis());

                            this.poolPairModelList.add(new PoolPairModel(entry.getKey(), (entry.getValue().getFiatCommissions1Value() * currentDFIPrice) + (entry.getValue().getFiatCommissions2Value() * currentPairPrice), entry.getValue().getCoinCommissions1Value(), entry.getValue().getCoinCommissions2Value(), this.settingsController.selectedCoin.getValue(), entry.getValue().getCoinCommissions1Value() * currentDFIPrice, entry.getValue().getCoinCommissions2Value() * currentPairPrice, 1.0, 1, ""));
                        } else {
                            this.poolPairModelList.add(new PoolPairModel(entry.getKey(), entry.getValue().getFiatCommissions1Value() + entry.getValue().getFiatCommissions2Value(), entry.getValue().getCoinCommissions1Value(), entry.getValue().getCoinCommissions2Value(), this.settingsController.selectedCoin.getValue(), entry.getValue().getFiatCommissions1Value(), entry.getValue().getFiatCommissions2Value(), 1.0, 1, ""));
                        }
                    }
                }

                if (this.mainView.plotCommissions1.getData().size() == 1) {
                    this.mainView.plotCommissions1.getData().remove(0);
                }

                if (this.mainView.plotCommissions2.getData().size() == 1) {
                    this.mainView.plotCommissions2.getData().remove(0);
                }

                this.mainView.plotCommissions1.getData().add(rewardsCumulated1);
                for (XYChart.Series<Number, Number> s : this.mainView.plotCommissions1.getData()) {
                    for (XYChart.Data d : s.getData()) {
                        Tooltip t = new Tooltip(d.getYValue().toString());
                        //t.setShowDelay(Duration.seconds(0));
                        Tooltip.install(d.getNode(), t);
                        d.getNode().setOnMouseEntered(event -> d.getNode().getStyleClass().add("onHover"));
                        d.getNode().setOnMouseExited(event -> d.getNode().getStyleClass().remove("onHover"));
                    }
                }

                this.mainView.plotCommissions2.getData().add(rewardsCumulated2);
                for (XYChart.Series<Number, Number> s : this.mainView.plotCommissions2.getData()) {
                    for (XYChart.Data d : s.getData()) {
                        Tooltip t = new Tooltip(d.getYValue().toString());
                        //t.setShowDelay(Duration.seconds(0));
                        Tooltip.install(d.getNode(), t);
                        d.getNode().setOnMouseEntered(event -> d.getNode().getStyleClass().add("onHover"));
                        d.getNode().setOnMouseExited(event -> d.getNode().getStyleClass().remove("onHover"));
                    }
                }

            }
            this.poolPairModelList.sort(Comparator.comparing(PoolPairModel::getBlockTimeValue));
            this.poolPairList.clear();
            this.poolPairList.addAll(this.poolPairModelList);
        }

    }

    public ObservableList<TransactionModel> getTransactionTable() {
        return this.transactionController.getTransactionList();
    }

    public ObservableList<PoolPairModel> getPlotData() {
        return this.poolPairList;
    }

    public void exportTransactionToExcel(List<TransactionModel> list, String filter) {

        Locale localeDecimal = Locale.GERMAN;
        if (settingsController.selectedDecimal.getValue().equals(".")) {
            localeDecimal = Locale.US;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV files", "*.csv")
        );
        if (new File(this.settingsController.lastExportPath).isDirectory()) {
            fileChooser.setInitialDirectory(new File(this.settingsController.lastExportPath));
        }

        String exportPath;
        Date date = new Date(System.currentTimeMillis());
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        if (filter.equals("DAILY")) {
            exportPath = dateFormat.format(date) + "_Portfolio_Export_RawData";
        } else if (filter.equals("")) {
            exportPath = dateFormat.format(date) + "_Portfolio_Export_RawData";
        }else {
            exportPath = dateFormat.format(date) + "_Portfolio_Export_Cointracking";
        }


        fileChooser.setInitialFileName(exportPath);
        File selectedFile = fileChooser.showSaveDialog(new Stage());

        if (selectedFile != null) {
            boolean success;
            if (filter.equals("DAILY")) {
                success = this.expService.exportTransactionToExcelDaily(list, selectedFile.getPath(), localeDecimal, this.settingsController.selectedSeperator.getValue());
            } else if (filter.equals("")) {
                success = this.expService.exportTransactionToExcel(list, selectedFile.getPath(), localeDecimal, this.settingsController.selectedSeperator.getValue());
            } else {
                success = this.expService.exportTransactionToCointracking(list, selectedFile.getPath(), localeDecimal, this.settingsController.selectedSeperator.getValue(), SettingsController.getInstance().exportCointracingVariante.getValue());
            }

            if (success) {
                this.settingsController.lastExportPath = selectedFile.getParent().toString();
                this.settingsController.saveSettings();
                try {
                    this.showExportSuccessfullWindow();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    this.showExportErrorWindow();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void exportTransactionToExcel(TableView<TransactionModel> rawDataTable) {
        List<TransactionModel> list;
        String filter;
        if(SettingsController.getInstance().exportCSVCariante.getValue().equals("Export selected to CSV")){
            list = rawDataTable.selectionModelProperty().get().getSelectedItems();
            filter = "";
        }else if(SettingsController.getInstance().exportCSVCariante.getValue().equals("Export all to CSV"))
        {
            list = rawDataTable.getItems();
            filter = "";
        }else{
            list = rawDataTable.getItems();
            filter = "DAILY";
        }

        Locale localeDecimal = Locale.GERMAN;
        if (settingsController.selectedDecimal.getValue().equals(".")) {
            localeDecimal = Locale.US;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV files", "*.csv")
        );
        if (new File(this.settingsController.lastExportPath).isDirectory()) {
            fileChooser.setInitialDirectory(new File(this.settingsController.lastExportPath));
        }

        Date date = new Date(System.currentTimeMillis());
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        fileChooser.setInitialFileName(dateFormat.format(date) + "_Portfolio_Export_RawData");
        File selectedFile = fileChooser.showSaveDialog(new Stage());

        if (selectedFile != null) {
            boolean success;
            if (filter.equals("DAILY")) {
                success = this.expService.exportTransactionToExcelDaily(list, selectedFile.getPath(), localeDecimal, this.settingsController.selectedSeperator.getValue());
            } else if (filter.equals("")) {
                success = this.expService.exportTransactionToExcel(list, selectedFile.getPath(), localeDecimal, this.settingsController.selectedSeperator.getValue());
            } else {
                success = this.expService.exportTransactionToCointracking(list, selectedFile.getPath(), localeDecimal, this.settingsController.selectedSeperator.getValue(), SettingsController.getInstance().exportCointracingVariante.getValue());
            }

            if (success) {
                this.settingsController.lastExportPath = selectedFile.getParent().toString();
                this.settingsController.saveSettings();
                try {
                    this.showExportSuccessfullWindow();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    this.showExportErrorWindow();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void exportPoolPairToExcel(List<PoolPairModel> list, String source) {

        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV files", "*.csv")
        );
        if (new File(this.settingsController.lastExportPath).isDirectory()) {
            fileChooser.setInitialDirectory(new File(this.settingsController.lastExportPath));
        }
        Date date = new Date(System.currentTimeMillis());
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        fileChooser.setInitialFileName(dateFormat.format(date) + "_Portfolio_Export_" + this.mainView.tabPane.getSelectionModel().getSelectedItem().getId());
        File selectedFile = fileChooser.showSaveDialog(new Stage());

        if (selectedFile != null) {
            boolean success = this.expService.exportPoolPairToExcel(list, selectedFile.getPath(), source, this.mainView);

            if (success) {
                this.settingsController.lastExportPath = selectedFile.getParent().toString();
                this.settingsController.saveSettings();
                try {
                    this.showExportSuccessfullWindow();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    this.showExportErrorWindow();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void showExportSuccessfullWindow() throws IOException {
        Parent rootExportFinished;
        rootExportFinished = FXMLLoader.load(getClass().getResource("../views/ExportSuccessfullView.fxml"));
        Scene sceneExportFinished = new Scene(rootExportFinished);
        Stage stageExportFinished = new Stage();
        stageExportFinished.setScene(sceneExportFinished);
        stageExportFinished.initStyle(StageStyle.UNDECORATED);
        sceneExportFinished.setOnMousePressed(mouseEvent -> {
            // record a delta distance for the drag and drop operation.
            dragDelta.x = stageExportFinished.getX() - mouseEvent.getScreenX();
            dragDelta.y = stageExportFinished.getY() - mouseEvent.getScreenY();
        });
        sceneExportFinished.setOnMouseDragged(mouseEvent -> {
            stageExportFinished.setX(mouseEvent.getScreenX() + dragDelta.x);
            stageExportFinished.setY(mouseEvent.getScreenY() + dragDelta.y);
        });
        stageExportFinished.show();
        stageExportFinished.setAlwaysOnTop(true);

        if (SettingsController.getInstance().selectedStyleMode.getValue().equals("Dark Mode")) {
            java.io.File darkMode = new File(System.getProperty("user.dir") + "/defi-portfolio/src/portfolio/styles/darkMode.css");
            stageExportFinished.getScene().getStylesheets().add(darkMode.toURI().toString());
        } else {
            java.io.File lightMode = new File(System.getProperty("user.dir") + "/defi-portfolio/src/portfolio/styles/lightMode.css");
            stageExportFinished.getScene().getStylesheets().add(lightMode.toURI().toString());
        }
    }
    public void showExportErrorWindow() throws IOException {
        Parent rootExportFinished;
        rootExportFinished = FXMLLoader.load(getClass().getResource("../views/ExportErrorView.fxml"));
        Scene sceneExportFinished = new Scene(rootExportFinished);
        Stage stageExportFinished = new Stage();
        stageExportFinished.setScene(sceneExportFinished);
        stageExportFinished.initStyle(StageStyle.UNDECORATED);
        sceneExportFinished.setOnMousePressed(mouseEvent -> {
            // record a delta distance for the drag and drop operation.
            dragDelta.x = stageExportFinished.getX() - mouseEvent.getScreenX();
            dragDelta.y = stageExportFinished.getY() - mouseEvent.getScreenY();
        });
        sceneExportFinished.setOnMouseDragged(mouseEvent -> {
            stageExportFinished.setX(mouseEvent.getScreenX() + dragDelta.x);
            stageExportFinished.setY(mouseEvent.getScreenY() + dragDelta.y);
        });
        stageExportFinished.show();
        stageExportFinished.setAlwaysOnTop(true);

        if (SettingsController.getInstance().selectedStyleMode.getValue().equals("Dark Mode")) {
            java.io.File darkMode = new File(System.getProperty("user.dir") + "/defi-portfolio/src/portfolio/styles/darkMode.css");
            stageExportFinished.getScene().getStylesheets().add(darkMode.toURI().toString());
        } else {
            java.io.File lightMode = new File(System.getProperty("user.dir") + "/defi-portfolio/src/portfolio/styles/lightMode.css");
            stageExportFinished.getScene().getStylesheets().add(lightMode.toURI().toString());
        }
    }
    static class Delta { double x, y; }
}
