package portfolio.controllers;

import javafx.beans.property.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Properties;
import java.util.Timer;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


public class SettingsController {
    public String Version = "V1.6.2";

    private static SettingsController OBJ = null;

    static {
        try {
            OBJ = new SettingsController();
        } catch (IOException e) {
            SettingsController.getInstance().logger.warning("Exception occured: " + e.toString());
        }
    }

    public StringProperty selectedLanguage = new SimpleStringProperty("English");
    public StringProperty selectedFiatCurrency = new SimpleStringProperty("EUR");
    public StringProperty selectedDecimal = new SimpleStringProperty(".");
    public StringProperty selectedSeperator = new SimpleStringProperty(",");
    public StringProperty selectedStyleMode = new SimpleStringProperty("Dark Mode");
    public StringProperty selectedCoin = new SimpleStringProperty("BTC-DFI");
    public StringProperty selectedPlotCurrency = new SimpleStringProperty("Coin");
    public StringProperty selectedDefaulUpdateSource = new SimpleStringProperty("Show options");
    public StringProperty selectedPlotType = new SimpleStringProperty();
    public StringProperty selectedIntervall = new SimpleStringProperty();
    public StringProperty tokenBalance = new SimpleStringProperty();
    public StringProperty tokenBalanceLM = new SimpleStringProperty();
    public StringProperty tokenAmount = new SimpleStringProperty();
    public StringProperty tokenYield = new SimpleStringProperty();
    public StringProperty tokenYieldRewards = new SimpleStringProperty();
    public StringProperty tokenYieldCommissions = new SimpleStringProperty();
    public ObjectProperty<LocalDate> dateFrom = new SimpleObjectProperty("2020-11-30");
    public ObjectProperty<LocalDate> dateTo = new SimpleObjectProperty();
    public ObjectProperty<LocalDate> exportFrom = new SimpleObjectProperty();
    public ObjectProperty<LocalDate> exportTo = new SimpleObjectProperty();
    public ObjectProperty<JSONObject> translationList = new SimpleObjectProperty();
    public String selectedIntervallInt = "Daily";
    public StringProperty selectedSource = new SimpleStringProperty("Active Wallet");
    public StringProperty exportCointracingVariante = new SimpleStringProperty();
    public StringProperty exportCSVCariante = new SimpleStringProperty();
    public boolean showDisclaim = true;
    public boolean showMissingTransaction = true;
    public boolean selectedLaunchDefid = false;
    public boolean selectedLaunchSync = true;
    public boolean checkCointracking = false;
    public boolean updatePython = false;
    public ArrayList<String> listAddresses = new ArrayList();

    public StringProperty lastUpdate = new SimpleStringProperty("-");
    //Combo box filling
    public String[] cryptoCurrencies;//{"BTC-DFI", "ETH-DFI", "USDT-DFI", "LTC-DFI", "BCH-DFI", "DOGE-DFI", "USDC-DFI","DUSD-DFI","TSLA-DUSD","SPY-DUSD","QQQ-DUSD","PLTR-DUSD","AAPL-DUSD","GME-DUSD","GOOGL-DUSD","BABA-DUSD","ARKK-DUSD","TLT-DUSD","GLD-DUSD","SLV-DUSD","PDBC-DUSD","URTH-DUSD","NVDA-DUSD","AMZN-DUSD","EEM-DUSD","COIN-DUSD","INTC-DUSD","DIS-DUSD","MSFT-DUSD","NFLX-DUSD","VOO-DUSD","MSTR-DUSD","FB-DUSD","MCHI-DUSD"};
    public String[] plotCurrency = new String[]{"Coin", "Daily Fiat", "Current Fiat"};
    public String[] styleModes = new String[]{"Light Mode", "Dark Mode"};
    public String[] datasources = new String[]{"Active Wallet", "All Wallets"};
    public String[] cointrackingExportVariants = new String[]{"Cumulate All", "Cumulate None","Cumulate Pool Pair","Cumulate Rewards and Commisions"};
    public String[] csvExportVariants = new String[]{"Export selected to CSV","Export all to CSV","Export all to CSV (Daily cumulated)"};
    public String[] defaultUpdateSource = new String[]{"Show options", "Update data","Wallet CSV"};

    //All relevant paths and files
    public String USER_HOME_PATH = System.getProperty("user.home").replace("\\","/");
    public String BINARY_FILE_NAME = getPlatform().equals("win") ? "defid.exe" : "defid";
    public String BINARY_FILE_PATH = System.getProperty("user.dir").replace("\\","/") + "/PortfolioData/" + BINARY_FILE_NAME;
    public String CONFIG_FILE_PATH =getPlatform() == "win" ?
            System.getProperty("user.home").replace("\\","/") + "/.defi/defi.conf" : //WIN PATH
    getPlatform() == "mac" ? System.getProperty("user.home").replace("\\","/")  + "/Library/Application Support/DeFi/.defi/defi.conf" : //MAC PATH
    getPlatform() == "linux" ? System.getProperty("user.home").replace("\\","/")  + "/.defi/defi.conf" : //LINUX PATH
    "";
    public String DEFI_PORTFOLIO_HOME = getPlatform().equals("win") ?
            System.getenv("APPDATA").replace("\\","/") + "/defi-portfolio/" : //WIN PATH
            getPlatform().equals("mac") ? System.getProperty("user.dir").replace("\\","/") + "/PortfolioData/" : //MAC PATH
                    getPlatform().equals("linux") ? System.getProperty("user.dir").replace("\\","/") + "/PortfolioData/" : //LINUX PATH;
                            "";
    public String PORTFOLIO_CONFIG_FILE_PATH = System.getProperty("user.dir").replace("\\","/") + "/PortfolioData/defi.conf";
    public String SETTING_FILE_PATH = DEFI_PORTFOLIO_HOME + "settings.csv";
    public String PORTFOLIO_FILE_PATH = DEFI_PORTFOLIO_HOME + "portfolioData.portfolio";
    public String INCOMPLETE_FILE_PATH = DEFI_PORTFOLIO_HOME + "incompleteList.portfolio";
    public String strTransactionData = "transactionData.portfolio";
    public String strCoinPriceData = "coinPriceData.portfolio";
    public String strStockPriceData = "stockTockenPrices.portfolio";
    public String strStockTokens = "stockTockens.portfolio";
    public String[] languages = new String[]{"English", "Deutsch","Espa\u00F1ol","Bokm\u00E5l","Nederlands"};
    public String[] currencies = new String[]{"EUR", "USD", "CHF"};
    public String[] decSeperators = new String[]{".", ","};
    public String[] csvSeperators = new String[]{",", ";"};
    public Logger logger = Logger.getLogger("Logger");
    public String rpcauth;
    public String rpcuser;
    public String rpcpassword;
    public String rpcbind;
    public String rpcport;

    public boolean runTimer = true;
    public boolean debouncer = false;
    public String auth;

    public Timer timer = new Timer("Timer");

    public String lastExportPath = USER_HOME_PATH;
    public String lastWalletCSVImportPath;
    public boolean runCheckTimer;
    public int errorBouncer = 0;

    private SettingsController() throws IOException {
        FileHandler fh;

        File directory = new File(DEFI_PORTFOLIO_HOME);
        if (!directory.exists()) {
            directory.mkdir();
        }

        fh = new FileHandler(DEFI_PORTFOLIO_HOME + "log.txt");
        this.logger.addHandler(fh);
        SimpleFormatter formatter = new SimpleFormatter();
        fh.setFormatter(formatter);
        this.loadSettings();
        this.loadAddresses();
        updateLanguage();
        getConfig();
        loadCryptoCurrencysList();
    }
    public void loadCryptoCurrencysList(){
        int nrTokens = 0;
        String savePath = this.DEFI_PORTFOLIO_HOME + this.strStockTokens;
        File f = new File(savePath);
        if(f.exists() && !f.isDirectory()) {
            try (BufferedReader br = new BufferedReader(new FileReader(savePath))) {
                String line;
                while ((line = br.readLine()) != null) {
                   nrTokens++;
                }
            }catch (IOException e) {
                e.printStackTrace();
            }
        }
        int i = 0;
        if(f.exists() && !f.isDirectory()) {
            cryptoCurrencies = new String[nrTokens];
            try (BufferedReader br = new BufferedReader(new FileReader(savePath))) {
                String line;
                while ((line = br.readLine()) != null) {
                    cryptoCurrencies[i]=line;
                    i++;
                }
            }catch (IOException e) {
                e.printStackTrace();
            }
        }
        int b = 0;
    }
    public void loadAddresses(){
        String savePath = this.DEFI_PORTFOLIO_HOME + "Addresses.csv";
        File f = new File(savePath);
        if(f.exists() && !f.isDirectory()) {
            this.listAddresses.clear();
            try (BufferedReader br = new BufferedReader(new FileReader(savePath))) {
                String line;
                while ((line = br.readLine()) != null) {
                    this.listAddresses.add(line);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public void updateLanguage() {

        JSONParser jsonParser = new JSONParser();

        String fileName = System.getProperty("user.dir").replace("\\","/") + "/defi-portfolio/src/portfolio/translations/";
        switch (selectedLanguage.getValue()) {
            case "English":
                fileName += "en.json";
                break;
            case "Deutsch":
                fileName += "de.json";
                break;
            case "Espa\u00F1ol":
                fileName += "es.json";
                break;
            case "Bokm\u00E5l":
                fileName += "nb.json";
                break;
            case "Nederlands":
                fileName += "dut.json";
                break;
            default:
                fileName += "en.json";
                break;
        }
        try (FileReader reader = new FileReader(fileName)) {
            Object obj = jsonParser.parse(reader);
            this.translationList.setValue((JSONObject) obj);
        } catch (ParseException | IOException e) {
            SettingsController.getInstance().logger.warning("Exception occured: " + e.toString());
        }
    }

    public static SettingsController getInstance() {
        return OBJ;
    }

    public String getPlatform() {
        String OS = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
        if ((OS.contains("mac")) || (OS.contains("darwin"))) {
            return "mac";
        } else if (OS.contains("win")) {
            return "win";
        } else if (OS.contains("nux")) {
            return "linux";
        } else {
            return "win";
        }
    }

    public void loadSettings() throws IOException {
        File f = new File(SETTING_FILE_PATH);
        if (f.exists() && !f.isDirectory()) {
            File configFile = new File(SETTING_FILE_PATH);
            Properties configProps = new Properties();
            try (FileInputStream i = new FileInputStream(configFile)) {
                configProps.load(i);
            }

            try {
                if(configProps.getProperty("SelectedLanguage").contains("Espa")) {
                    this.selectedLanguage.setValue("Espa\u00F1ol");
                }
                else if(configProps.getProperty("SelectedLanguage").contains("Bokm")){
                    this.selectedLanguage.setValue("Bokm\u00E5l");
                }else{
                    this.selectedLanguage.setValue(configProps.getProperty("SelectedLanguage"));
                }
                this.selectedFiatCurrency.setValue(configProps.getProperty("SelectedFiatCurrency"));
                this.selectedDecimal.setValue(configProps.getProperty("SelectedDecimal"));
                this.selectedSeperator.setValue(configProps.getProperty("SelectedSeperator"));
                this.selectedCoin.setValue(configProps.getProperty("SelectedCoin"));
                this.selectedPlotCurrency.setValue(configProps.getProperty("SelectedPlotCurrency"));
                this.selectedStyleMode.setValue(configProps.getProperty("SelectedStyleMode"));
                this.exportCointracingVariante.setValue(configProps.getProperty("ExportCointrackinVariante"));
                this.exportCSVCariante.setValue(configProps.getProperty("ExportCSVVariante"));
                this.exportFrom.setValue(LocalDate.parse(configProps.getProperty("ExportFrom")));
                this.exportTo.setValue(LocalDate.parse(LocalDate.now().toString()));
                this.dateFrom.setValue(LocalDate.parse(configProps.getProperty("SelectedDate")));
                if (!configProps.getProperty("LastUsedExportPath").equals(""))
                    this.lastExportPath = configProps.getProperty("LastUsedExportPath");
                this.showDisclaim = configProps.getProperty("ShowDisclaim").equals("true");
                this.selectedLaunchDefid = configProps.getProperty("SelectedLaunchDefid").equals("true");
                if (configProps.getProperty("SelectedLaunchSync") != null) {
                    this.selectedLaunchSync = configProps.getProperty("SelectedLaunchSync").equals("true");
                } else {
                    this.selectedLaunchSync = false;
                }
                if(configProps.getProperty("SelectedSource") !=null){
                    this.selectedSource.setValue(configProps.getProperty("SelectedSource"));
                }else{
                    this.selectedSource.setValue("Active Wallet");
                }
                if(configProps.getProperty("LastUpdate") !=null){
                    this.lastUpdate.setValue(configProps.getProperty("LastUpdate"));
                }else{
                    this.lastUpdate.setValue("-");
                }
                if(configProps.getProperty("DefaultDataSource") !=null){
                    this.selectedDefaulUpdateSource.setValue(configProps.getProperty("DefaultDataSource"));
                }else{
                    this.selectedDefaulUpdateSource.setValue("Show options");
                }
                this.showMissingTransaction = configProps.getProperty("MissingTransaction").equals("true");
                this.lastWalletCSVImportPath = configProps.getProperty("LastWalletCSVImportPath");
            } catch (Exception e) {
                SettingsController.getInstance().logger.warning("Exception occured: " + e.toString());
                saveSettings();
            }
        }
    }

    public void saveSettings() {

        FileWriter csvWriter;
        try {
            csvWriter = new FileWriter(SETTING_FILE_PATH);
            csvWriter.append("SelectedLanguage=" + this.selectedLanguage.getValue()).append("\n");
            csvWriter.append("SelectedFiatCurrency=" + this.selectedFiatCurrency.getValue()).append("\n");
            csvWriter.append("SelectedDecimal=" + this.selectedDecimal.getValue()).append("\n");
            csvWriter.append("SelectedSeperator=" + this.selectedSeperator.getValue()).append("\n");
            csvWriter.append("SelectedCoin=" + this.selectedCoin.getValue()).append("\n");
            csvWriter.append("SelectedPlotCurrency=" + this.selectedPlotCurrency.getValue()).append("\n");
            csvWriter.append("SelectedStyleMode=" + this.selectedStyleMode.getValue()).append("\n");
            csvWriter.append("SelectedDate=" + this.dateFrom.getValue()).append("\n");
            csvWriter.append("LastUsedExportPath=" + this.lastExportPath.replace("\\","/")).append("\n");
            csvWriter.append("ShowDisclaim=" + this.showDisclaim).append("\n");
            csvWriter.append("SelectedLaunchDefid=" + this.selectedLaunchDefid).append("\n");
            csvWriter.append("SelectedLaunchSync=" + this.selectedLaunchSync).append("\n");
            csvWriter.append("SelectedSource=" + this.selectedSource.getValue()).append("\n");
            csvWriter.append("LastUpdate=" + this.lastUpdate.getValue()).append("\n");
            csvWriter.append("ExportCointrackinVariante=" + this.exportCointracingVariante.getValue()).append("\n");
            csvWriter.append("ExportCSVVariante=" + this.exportCSVCariante.getValue()).append("\n");
            csvWriter.append("ExportFrom=" + this.exportFrom.getValue()).append("\n");
            csvWriter.append("ExportTo=" + this.exportTo.getValue()).append("\n");
            csvWriter.append("MissingTransaction=" + this.showMissingTransaction).append("\n");
            csvWriter.append("DefaultDataSource=" + this.selectedDefaulUpdateSource.getValue()).append("\n");
            csvWriter.append("LastWalletCSVImportPath=" + this.lastWalletCSVImportPath).append("\n");
            csvWriter.flush();
            csvWriter.close();
        } catch (IOException e) {
            this.logger.warning("Exception occured: " + e.toString());
        }
    }

    public void getConfig() {

        // copy config file
        try {
            File pathConfig = new File(this.CONFIG_FILE_PATH);
            if (pathConfig.exists()) {
                File pathPortfoliohDataConfig = new File(this.PORTFOLIO_CONFIG_FILE_PATH);
                Files.copy(pathConfig.toPath(), pathPortfoliohDataConfig.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            SettingsController.getInstance().logger.warning("Exception occured: " + e.toString());
        }
        // adapt port
        Path path = Paths.get(this.PORTFOLIO_CONFIG_FILE_PATH);
        Charset charset = StandardCharsets.UTF_8;
        try {
            File configFile = new File(this.PORTFOLIO_CONFIG_FILE_PATH);
            Properties configProps = new Properties();
            try (FileInputStream i = new FileInputStream(configFile)) {
                configProps.load(i);
            } catch (IOException e) {
                SettingsController.getInstance().logger.warning("Exception occured: " + e.toString());
            }

            String rpcportConfig = configProps.getProperty("rpcport");
            String rpcBindConfig = configProps.getProperty("rpcbind");
            String rpcConnectConfig = configProps.getProperty("rpcconnect");
            String content = new String(Files.readAllBytes(path), charset);
            if(rpcportConfig != null)content = content.replaceAll(rpcportConfig, "8554");
            if(rpcBindConfig != null)content = content.replaceAll(rpcBindConfig, "127.0.0.1");
            if(rpcConnectConfig != null)content = content.replaceAll(rpcConnectConfig, "127.0.0.1");
            Files.write(path, content.getBytes(charset));
        } catch (Exception e) {
            SettingsController.getInstance().logger.warning("Exception occured: " + e.toString());
        }

            File configFile = new File(this.PORTFOLIO_CONFIG_FILE_PATH);
            Properties configProps = new Properties();
            try (FileInputStream i = new FileInputStream(configFile)) {
                configProps.load(i);
            } catch (IOException e) {
                SettingsController.getInstance().logger.warning("Exception occured: " + e.toString());
            }
            this.rpcauth = configProps.getProperty("rpcauth");
            this.rpcuser = configProps.getProperty("rpcuser");
            this.rpcpassword = configProps.getProperty("rpcpassword");
            this.rpcbind = configProps.getProperty("rpcbind");
            this.rpcport = configProps.getProperty("rpcport");
            this.auth = this.rpcuser + ":" + this.rpcpassword;

    }
}