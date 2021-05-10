package portfolio.services;

import javafx.scene.chart.PieChart;
import javafx.scene.control.TableColumn;
import portfolio.controllers.MainViewController;
import portfolio.controllers.SettingsController;
import portfolio.controllers.TransactionController;
import portfolio.models.PortfolioModel;
import portfolio.views.MainView;
import portfolio.models.PoolPairModel;
import portfolio.models.TransactionModel;

import java.io.*;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

public class ExportService {

    MainViewController mainViewController;

    public ExportService(MainViewController mainViewController) {
        this.mainViewController = mainViewController;
    }

    public boolean exportTransactionToExcel(List<TransactionModel> transactions, String exportPath, Locale localeDecimal, String exportSplitter) {
        File exportFile = new File(exportPath);
        this.mainViewController.settingsController.lastExportPath = exportFile.getParent();
        this.mainViewController.settingsController.saveSettings();
        if (exportFile.exists()) exportFile.delete();

        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileWriter(exportPath, true));
        } catch (IOException e) {
            SettingsController.getInstance().logger.warning("Exception occured: " + e.toString());
        }
        StringBuilder sb = new StringBuilder();

        for (TableColumn column : this.mainViewController.mainView.rawDataTable.getColumns()
        ) {
            sb.append(column.getId()).append(this.mainViewController.settingsController.selectedSeperator.getValue());
        }

        sb.setLength(sb.length() - 1);
        sb.append("\n");
        writer.write(sb.toString());

        for (TransactionModel transaction : transactions) {
            sb = new StringBuilder();
            sb.append(this.mainViewController.transactionController.convertTimeStampToString(transaction.blockTimeProperty.getValue())).append(exportSplitter);
            sb.append(transaction.typeProperty.getValue()).append(exportSplitter);
            sb.append(String.format(localeDecimal, "%.8f", transaction.cryptoValueProperty.getValue())).append(exportSplitter);
            sb.append(transaction.cryptoCurrencyProperty.getValue()).append(exportSplitter);
            sb.append(String.format(localeDecimal, "%.8f", transaction.fiatValueProperty.getValue())).append(exportSplitter);
            sb.append(transaction.fiatCurrencyProperty.getValue()).append(exportSplitter);
            sb.append(transaction.poolIDProperty.getValue()).append(exportSplitter);
            sb.append(transaction.blockHeightProperty.getValue()).append(exportSplitter);
            sb.append(transaction.blockHashProperty.getValue()).append(exportSplitter);
            sb.append(transaction.ownerProperty.getValue()).append(exportSplitter);
            sb.append(transaction.txIDProperty.getValue());
            sb.append("\n");
            writer.write(sb.toString());
            sb = null;
        }
        writer.close();
        return true;

    }

    public boolean exportTransactionToExcelDaily(List<TransactionModel> transactions, String exportPath, Locale localeDecimal, String exportSplitter) {
        File exportFile = new File(exportPath);
        this.mainViewController.settingsController.lastExportPath = exportFile.getParent();
        this.mainViewController.settingsController.saveSettings();
        if (exportFile.exists()) exportFile.delete();

        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileWriter(exportPath, true));
        } catch (IOException e) {
            SettingsController.getInstance().logger.warning("Exception occured: " + e.toString());
        }
        StringBuilder sb = new StringBuilder();

        for (TableColumn column : this.mainViewController.mainView.rawDataTable.getColumns()
        ) {
            sb.append(column.getId()).append(this.mainViewController.settingsController.selectedSeperator.getValue());
        }

        sb.setLength(sb.length() - 1);
        sb.append("\n");
        writer.write(sb.toString());
        TreeMap<String, TransactionModel> exportList = new TreeMap<>();
        String oldDate = "";
        for (TransactionModel transaction : transactions) {
            String newDate = this.mainViewController.transactionController.convertTimeStampWithoutTimeToString(transaction.blockTimeProperty.getValue());

            if (transaction.typeProperty.getValue().equals("Commission") || transaction.typeProperty.getValue().equals("Rewards")) {

                if ((oldDate.equals("") || oldDate.equals(newDate))) {
                    String key = this.mainViewController.transactionController.getPoolPairFromId(transaction.poolIDProperty.getValue()) + transaction.cryptoCurrencyProperty.getValue() + transaction.typeProperty.getValue();
                    if (!exportList.containsKey(key)) {
                        exportList.put(key, new TransactionModel(transaction.blockTimeProperty.getValue(), transaction.ownerProperty.getValue(), transaction.typeProperty.getValue(), transaction.amountProperty.getValue(), transaction.blockHashProperty.getValue(), transaction.blockHeightProperty.getValue(), transaction.poolIDProperty.getValue(), transaction.txIDProperty.getValue(), this.mainViewController.transactionController));
                    } else {
                        exportList.get(key).cryptoValueProperty.set(exportList.get(key).cryptoValueProperty.getValue() + transaction.cryptoValueProperty.getValue());
                        exportList.get(key).fiatValueProperty.set(exportList.get(key).fiatValueProperty.getValue() + transaction.fiatValueProperty.getValue());
                    }
                } else {
                    for (HashMap.Entry<String, TransactionModel> entry : exportList.entrySet()) {

                        sb = new StringBuilder();
                        sb.append(this.mainViewController.transactionController.convertTimeStampWithoutTimeToString(entry.getValue().blockTimeProperty.getValue())).append(exportSplitter);
                        sb.append(entry.getValue().typeProperty.getValue()).append(exportSplitter);
                        sb.append(String.format(localeDecimal, "%.8f", entry.getValue().cryptoValueProperty.getValue())).append(exportSplitter);
                        sb.append(entry.getValue().cryptoCurrencyProperty.getValue()).append(exportSplitter);
                        sb.append(String.format(localeDecimal, "%.8f", entry.getValue().fiatValueProperty.getValue())).append(exportSplitter);
                        sb.append(entry.getValue().fiatCurrencyProperty.getValue()).append(exportSplitter);
                        sb.append(entry.getValue().poolIDProperty.getValue()).append(exportSplitter);
                        sb.append(entry.getValue().blockHeightProperty.getValue()).append(exportSplitter);
                        sb.append(entry.getValue().blockHashProperty.getValue()).append(exportSplitter);
                        sb.append(entry.getValue().ownerProperty.getValue()).append(exportSplitter);
                        sb.append(entry.getValue().txIDProperty.getValue());
                        sb.append("\n");
                        writer.write(sb.toString());
                        sb = null;

                    }
                    exportList = new TreeMap<>();

                    String key = this.mainViewController.transactionController.getPoolPairFromId(transaction.poolIDProperty.getValue()) + transaction.cryptoCurrencyProperty.getValue() + transaction.typeProperty.getValue();
                    exportList.put(key, new TransactionModel(transaction.blockTimeProperty.getValue(), transaction.ownerProperty.getValue(), transaction.typeProperty.getValue(), transaction.amountProperty.getValue(), transaction.blockHashProperty.getValue(), transaction.blockHeightProperty.getValue(), transaction.poolIDProperty.getValue(), transaction.txIDProperty.getValue(), this.mainViewController.transactionController));

                }

            } else {
                sb = new StringBuilder();
                sb.append(this.mainViewController.transactionController.convertTimeStampToString(transaction.blockTimeProperty.getValue())).append(exportSplitter);
                sb.append(transaction.typeProperty.getValue()).append(exportSplitter);
                sb.append(String.format(localeDecimal, "%.8f", transaction.cryptoValueProperty.getValue())).append(exportSplitter);
                sb.append(transaction.cryptoCurrencyProperty.getValue()).append(exportSplitter);
                sb.append(String.format(localeDecimal, "%.8f", transaction.fiatValueProperty.getValue())).append(exportSplitter);
                sb.append(transaction.fiatCurrencyProperty.getValue()).append(exportSplitter);
                sb.append(transaction.poolIDProperty.getValue()).append(exportSplitter);
                sb.append(transaction.blockHeightProperty.getValue()).append(exportSplitter);
                sb.append(transaction.blockHashProperty.getValue()).append(exportSplitter);
                sb.append(transaction.ownerProperty.getValue()).append(exportSplitter);
                sb.append(transaction.txIDProperty.getValue());
                sb.append("\n");
                writer.write(sb.toString());
                sb = null;
            }

            oldDate = newDate;
        }

        for (HashMap.Entry<String, TransactionModel> entry : exportList.entrySet()) {

            sb = new StringBuilder();
            sb.append(this.mainViewController.transactionController.convertTimeStampWithoutTimeToString(entry.getValue().blockTimeProperty.getValue())).append(exportSplitter);
            sb.append(entry.getValue().typeProperty.getValue()).append(exportSplitter);
            sb.append(String.format(localeDecimal, "%.8f", entry.getValue().cryptoValueProperty.getValue())).append(exportSplitter);
            sb.append(entry.getValue().cryptoCurrencyProperty.getValue()).append(exportSplitter);
            sb.append(String.format(localeDecimal, "%.8f", entry.getValue().fiatValueProperty.getValue())).append(exportSplitter);
            sb.append(entry.getValue().fiatCurrencyProperty.getValue()).append(exportSplitter);
            sb.append(entry.getValue().poolIDProperty.getValue()).append(exportSplitter);
            sb.append(entry.getValue().blockHeightProperty.getValue()).append(exportSplitter);
            sb.append(entry.getValue().blockHashProperty.getValue()).append(exportSplitter);
            sb.append(entry.getValue().ownerProperty.getValue()).append(exportSplitter);
            sb.append(entry.getValue().txIDProperty.getValue());
            sb.append("\n");
            writer.write(sb.toString());
            sb = null;

        }
        writer.close();
        exportList.clear();
        return true;

    }

    public String[] cointrackingExportVariants = new String[]{"Cumulate All", "Cumulate None","Cumulate Pool Pair","Cumulate Rewards and Commisions"};
    public boolean exportTransactionToCointracking(List<TransactionModel> transactions, String exportPath, Locale localeDecimal, String exportSplitter, String filter) {
        File exportFile = new File(exportPath);
        this.mainViewController.settingsController.lastExportPath = exportFile.getParent();
        this.mainViewController.settingsController.saveSettings();
        if (exportFile.exists()) exportFile.delete();

        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileWriter(exportPath, true));
        } catch (IOException e) {
            SettingsController.getInstance().logger.warning("Exception occured: " + e.toString());
        }

        long currentTimeStamp = (new Timestamp(System.currentTimeMillis()).getTime() - 24 * 60 * 60 * 1000) / 1000L;
        String yesterdayDate = TransactionController.getInstance().convertTimeStampYesterdayToString(currentTimeStamp);
        Timestamp ts = Timestamp.valueOf(yesterdayDate);

        StringBuilder sb = new StringBuilder();

        sb.append("Type,Buy Amount,Buy Currency,Sell Amount,Sell currency,Fee Amount,Fee Currency,Exchange,Trade Group,Comment,Date,Tx-ID,Buy Value in your Account Currency,Sell Value in your Account Currency");
        sb.setLength(sb.length() - 1);
        writer.write(sb.toString());
        TreeMap<String, TransactionModel> exportList = new TreeMap<>();
        String oldDate = "";
        int transCounter = 0;

        if (SettingsController.getInstance().checkCointracking) {
            for (TransactionModel transactionModel : transactions) {
                transactionModel.exportCointracking = false;
            }
        }

        for (TransactionModel transaction : transactions) {

            if (transaction.blockTimeProperty.getValue() * 1000L < ts.getTime()) {

                String newDate = this.mainViewController.transactionController.convertTimeStampWithoutTimeToString(transaction.blockTimeProperty.getValue());

                if (transaction.typeProperty.getValue().equals("Commission") || transaction.typeProperty.getValue().equals("Rewards")) {

                    if ((oldDate.equals("") || oldDate.equals(newDate))) {
                        String key = "";

                        switch (filter) {
                            case "Cumulate All":
                                key = transaction.cryptoCurrencyProperty.getValue();
                                break;
                            case "Cumulate Rewards and Commisions":
                                key = this.mainViewController.transactionController.getPoolPairFromId(transaction.poolIDProperty.getValue()) + transaction.cryptoCurrencyProperty.getValue();
                                break;
                            case "Cumulate Pool Pair":
                                key = transaction.cryptoCurrencyProperty.getValue() + transaction.typeProperty.getValue();
                                break;
                            case "Cumulate None":
                                key = this.mainViewController.transactionController.getPoolPairFromId(transaction.poolIDProperty.getValue()) + transaction.cryptoCurrencyProperty.getValue() + transaction.typeProperty.getValue();
                                break;
                        }

                        if (!exportList.containsKey(key)) {
                            exportList.put(key, new TransactionModel(transaction.blockTimeProperty.getValue(), transaction.ownerProperty.getValue(), transaction.typeProperty.getValue(), transaction.amountProperty.getValue(), transaction.blockHashProperty.getValue(), transaction.blockHeightProperty.getValue(), transaction.poolIDProperty.getValue(), transaction.txIDProperty.getValue(), this.mainViewController.transactionController));
                        } else {
                            exportList.get(key).cryptoValueProperty.set(exportList.get(key).cryptoValueProperty.getValue() + transaction.cryptoValueProperty.getValue());
                            exportList.get(key).fiatValueProperty.set(exportList.get(key).fiatValueProperty.getValue() + transaction.fiatValueProperty.getValue());
                        }
                    } else {

                        for (HashMap.Entry<String, TransactionModel> entry : exportList.entrySet()) {

                            sb = new StringBuilder();

                            sb.append("\n");
                            sb.append("\"" + Type2CointrackingType(entry.getValue().typeProperty.getValue()) + "\"").append(exportSplitter);
                            sb.append("\""+String.format(localeDecimal, "%.8f", entry.getValue().cryptoValueProperty.getValue())+"\"").append(exportSplitter);
                            sb.append("\""+entry.getValue().cryptoCurrencyProperty.getValue()+"\"").append(exportSplitter);
                            sb.append("\"\"").append(exportSplitter);
                            sb.append("\"\"").append(exportSplitter);
                            sb.append("\"\"").append(exportSplitter);
                            sb.append("\"\"").append(exportSplitter);
                            sb.append("\"DeFiChain-Wallet\"").append(exportSplitter);
                            sb.append("\"\"").append(exportSplitter);

                            switch (filter) {
                                case "Cumulate All":
                                    sb.append("\""+"LM Interest Income"+"\"").append(exportSplitter);
                                    break;
                                case "Cumulate Rewards and Commisions":
                                    sb.append("\""+"LM Interest Income (" + this.mainViewController.transactionController.getPoolPairFromId(entry.getValue().poolIDProperty.getValue())).append(")"+"\"").append(exportSplitter);
                                    break;
                                case "Cumulate Pool Pair":
                                    sb.append("\""+"LM " + entry.getValue().typeProperty.getValue()+"\"").append(exportSplitter);
                                    break;
                                case "Cumulate None":
                                    sb.append("\""+"LM " + entry.getValue().typeProperty.getValue() + " (" + this.mainViewController.transactionController.getPoolPairFromId(entry.getValue().poolIDProperty.getValue())).append(")"+"\"").append(exportSplitter);
                                    break;
                            }

                            sb.append("\""+TransactionController.getInstance().convertTimeStampToCointracking(entry.getValue().blockTimeProperty.getValue())+"\"").append(exportSplitter);
                            sb.append("\""+entry.getValue().cryptoCurrencyProperty.getValue() + TransactionController.getInstance().convertTimeStampWithoutTimeToString(entry.getValue().blockTimeProperty.getValue())+"\"").append(exportSplitter);
                            sb.append("\"\"").append(exportSplitter);
                            sb.append("\"\"");
                            writer.write(sb.toString());
                            sb = null;

                        }

                        exportList = new TreeMap<>();
                        String key = "";

                        switch (filter) {
                            case "Cumulate All":
                                key = transaction.cryptoCurrencyProperty.getValue();
                                break;
                            case "Cumulate Rewards and Commisions":
                                key = this.mainViewController.transactionController.getPoolPairFromId(transaction.poolIDProperty.getValue()) + transaction.cryptoCurrencyProperty.getValue();
                                break;
                            case "Cumulate Pool Pair":
                                key = transaction.cryptoCurrencyProperty.getValue() + transaction.typeProperty.getValue();
                                break;
                            case "Cumulate None":
                                key = this.mainViewController.transactionController.getPoolPairFromId(transaction.poolIDProperty.getValue()) + transaction.cryptoCurrencyProperty.getValue() + transaction.typeProperty.getValue();
                                break;
                        }
                        exportList.put(key, new TransactionModel(transaction.blockTimeProperty.getValue(), transaction.ownerProperty.getValue(), transaction.typeProperty.getValue(), transaction.amountProperty.getValue(), transaction.blockHashProperty.getValue(), transaction.blockHeightProperty.getValue(), transaction.poolIDProperty.getValue(), transaction.txIDProperty.getValue(), this.mainViewController.transactionController));
                    }

                } else {


                    if (!(transaction.typeProperty.getValue().equals("UtxosToAccount") || transaction.typeProperty.getValue().equals("AccountToUtxos"))) {

                        TransactionModel poolSwap2 = null;

                        if (transaction.typeProperty.getValue().equals("PoolSwap") && !transaction.exportCointracking) {
                            for (int i = transCounter; i < transactions.size(); i++) {
                                if (transactions.get(i).blockHeightProperty.getValue() > transaction.blockHeightProperty.getValue())
                                    break;

                                if (transactions.get(i).txIDProperty.getValue().equals(transaction.txIDProperty.getValue()) && !transactions.get(i).cryptoCurrencyProperty.getValue().equals(transaction.cryptoCurrencyProperty.getValue())) {
                                    poolSwap2 = transactions.get(i);
                                    transactions.get(i).exportCointracking = true;
                                    break;
                                }
                            }

                            if (poolSwap2 != null) {

                                sb = new StringBuilder();
                                sb.append("\n");
                                sb.append("\"Trade\"").append(exportSplitter);
                                if (transaction.cryptoValueProperty.getValue() > 0.0) {
                                    sb.append("\""+String.format(localeDecimal, "%.8f", transaction.cryptoValueProperty.getValue())+"\"").append(exportSplitter);
                                    sb.append("\""+transaction.cryptoCurrencyProperty.getValue()+"\"").append(exportSplitter);
                                    sb.append("\""+String.format(localeDecimal, "%.8f", poolSwap2.cryptoValueProperty.getValue() * -1)+"\"").append(exportSplitter);
                                    sb.append("\""+poolSwap2.cryptoCurrencyProperty.getValue()+"\"").append(exportSplitter);
                                } else {
                                    sb.append("\""+String.format(localeDecimal, "%.8f", poolSwap2.cryptoValueProperty.getValue())+"\"").append(exportSplitter);
                                    sb.append("\""+poolSwap2.cryptoCurrencyProperty.getValue()+"\"").append(exportSplitter);
                                    sb.append("\""+String.format(localeDecimal, "%.8f", transaction.cryptoValueProperty.getValue() * -1)+"\"").append(exportSplitter);
                                    sb.append("\""+transaction.cryptoCurrencyProperty.getValue()+"\"").append(exportSplitter);
                                }
                                sb.append("\"\"").append(exportSplitter);
                                sb.append("\"\"").append(exportSplitter);
                                sb.append("\"DeFiChain-Wallet\"").append(exportSplitter);
                                sb.append("\"\"").append(exportSplitter);
                                sb.append("\"\"").append(exportSplitter);
                                sb.append("\""+TransactionController.getInstance().convertTimeStampToCointrackingReal(transaction.blockTimeProperty.getValue())+"\"").append(exportSplitter);
                                sb.append("\"").append(transaction.txIDProperty.getValue()).append("\"").append(exportSplitter);
                                sb.append("\"\"").append(exportSplitter);
                                sb.append("\"\"");
                                writer.write(sb.toString());
                                sb = null;
                            }

                        }

                        TransactionModel addPool1 = null;
                        TransactionModel addPool2 = null;
                        TransactionModel addPool = null;

                        if (transaction.typeProperty.getValue().equals("AddPoolLiquidity") && !transaction.exportCointracking) {

                            for (int i = transCounter; i < transactions.size(); i++) {
                                if (transactions.get(i).blockHeightProperty.getValue() > transaction.blockHeightProperty.getValue())
                                    break;

                                if (transactions.get(i).txIDProperty.getValue().equals(transaction.txIDProperty.getValue())) {
                                    if (transactions.get(i).cryptoCurrencyProperty.getValue().equals("DFI")) {
                                        addPool1 = transactions.get(i);
                                        transaction.exportCointracking = true;
                                    }
                                    if (!transactions.get(i).cryptoCurrencyProperty.getValue().equals("DFI") && !transactions.get(i).cryptoCurrencyProperty.getValue().contains("-")) {
                                        addPool2 = transactions.get(i);
                                        transaction.exportCointracking = true;
                                    }
                                    if (transactions.get(i).cryptoCurrencyProperty.getValue().contains("-")) {
                                        addPool = transactions.get(i);
                                        transaction.exportCointracking = true;
                                    }
                                }
                                if (addPool != null && addPool2 != null && addPool1 != null) break;
                            }

                            if (addPool != null && addPool2 != null && addPool1 != null) {

                                sb = new StringBuilder();

                                sb.append("\n");
                                sb.append("\"Trade\"").append(exportSplitter);
                                sb.append("\""+String.format(localeDecimal, "%.8f", addPool.cryptoValueProperty.getValue() / 2)+"\"").append(exportSplitter);
                                sb.append("\""+TransactionController.getInstance().getPoolPairFromId(addPool.poolIDProperty.getValue())+"\"").append(exportSplitter);
                                sb.append("\""+String.format(localeDecimal, "%.8f", addPool1.cryptoValueProperty.getValue() * -1)+"\"").append(exportSplitter);
                                sb.append("\""+addPool1.cryptoCurrencyProperty.getValue()+"\"").append(exportSplitter);
                                sb.append("\"\"").append(exportSplitter);
                                sb.append("\"\"").append(exportSplitter);
                                sb.append("\"DeFiChain-Wallet\"").append(exportSplitter);
                                sb.append("\"\"").append(exportSplitter);
                                sb.append("\"Add-Pool-Liquidity (" + TransactionController.getInstance().getPoolPairFromId(addPool.poolIDProperty.getValue()) + ")\"").append(exportSplitter);
                                sb.append("\""+TransactionController.getInstance().convertTimeStampToCointrackingReal(addPool1.blockTimeProperty.getValue())+"\"").append(exportSplitter);
                                sb.append("\"").append(addPool1.txIDProperty.getValue()).append("\"").append(exportSplitter);
                                sb.append("\"").append(addPool1.fiatValueProperty.getValue()*-1).append("\"").append(exportSplitter);
                                sb.append("\"").append(addPool1.fiatValueProperty.getValue()*-1).append("\"");
                                writer.write(sb.toString());
                                sb = null;

                                sb = new StringBuilder();

                                sb.append("\n");
                                sb.append("\"Trade\"").append(exportSplitter);
                                sb.append("\""+String.format(localeDecimal, "%.8f", addPool.cryptoValueProperty.getValue() / 2)+"\"").append(exportSplitter);
                                sb.append("\""+TransactionController.getInstance().getPoolPairFromId(addPool.poolIDProperty.getValue())+"\"").append(exportSplitter);
                                sb.append("\""+String.format(localeDecimal, "%.8f", addPool2.cryptoValueProperty.getValue() * -1)+"\"").append(exportSplitter);
                                sb.append("\""+addPool2.cryptoCurrencyProperty.getValue()+"\"").append(exportSplitter);
                                sb.append("\"\"").append(exportSplitter);
                                sb.append("\"\"").append(exportSplitter);
                                sb.append("\"DeFiChain-Wallet\"").append(exportSplitter);
                                sb.append("\"\"").append(exportSplitter);
                                sb.append("\"Add-Pool-Liquidity (" + TransactionController.getInstance().getPoolPairFromId(addPool.poolIDProperty.getValue()) + ")\"").append(exportSplitter);
                                sb.append("\""+TransactionController.getInstance().convertTimeStampToCointrackingReal(addPool2.blockTimeProperty.getValue())+"\"").append(exportSplitter);
                                sb.append("\"").append(addPool2.txIDProperty.getValue()).append("\"").append(exportSplitter);
                                sb.append("\"").append(addPool2.fiatValueProperty.getValue()*-1).append("\"").append(exportSplitter);
                                sb.append("\"").append(addPool2.fiatValueProperty.getValue()*-1).append("\"");
                                writer.write(sb.toString());
                                sb = null;

                            }
                        }
                    }

                    TransactionModel addPool1 = null;
                    TransactionModel addPool2 = null;
                    TransactionModel addPool = null;

                    if (transaction.typeProperty.getValue().equals("RemovePoolLiquidity") && !transaction.exportCointracking) {

                        for (int i = transCounter; i < transactions.size(); i++) {
                            if (transactions.get(i).blockHeightProperty.getValue() > transaction.blockHeightProperty.getValue())
                                break;

                            if (transactions.get(i).txIDProperty.getValue().equals(transaction.txIDProperty.getValue())) {
                                if (transactions.get(i).cryptoCurrencyProperty.getValue().equals("DFI")) {
                                    addPool1 = transactions.get(i);
                                    transaction.exportCointracking = true;
                                }
                                if (!transactions.get(i).cryptoCurrencyProperty.getValue().equals("DFI") && !transactions.get(i).cryptoCurrencyProperty.getValue().contains("-")) {
                                    addPool2 = transactions.get(i);
                                    transaction.exportCointracking = true;
                                }
                                if (transactions.get(i).cryptoCurrencyProperty.getValue().contains("-")) {
                                    addPool = transactions.get(i);
                                    transaction.exportCointracking = true;
                                }
                            }
                            if (addPool != null && addPool2 != null && addPool1 != null) break;
                        }

                        if (addPool != null && addPool2 != null && addPool1 != null) {

                            sb = new StringBuilder();
                            sb.append("\n");
                            sb.append("\"Trade\"").append(exportSplitter);
                            sb.append("\""+String.format(localeDecimal, "%.8f", addPool1.cryptoValueProperty.getValue())+"\"").append(exportSplitter);
                            sb.append("\""+addPool1.cryptoCurrencyProperty.getValue()+"\"").append(exportSplitter);
                            sb.append("\""+String.format(localeDecimal, "%.8f", (addPool.cryptoValueProperty.getValue()/ 2) * -1)+"\"").append(exportSplitter);
                            sb.append("\""+TransactionController.getInstance().getPoolPairFromId(addPool.poolIDProperty.getValue())+"\"").append(exportSplitter);
                            sb.append("\"\"").append(exportSplitter);
                            sb.append("\"\"").append(exportSplitter);
                            sb.append("\"DeFiChain-Wallet\"").append(exportSplitter);
                            sb.append("\"\"").append(exportSplitter);
                            sb.append("\"Remove-Pool-Liquidity (" + TransactionController.getInstance().getPoolPairFromId(addPool.poolIDProperty.getValue()) + ")\"").append(exportSplitter);
                            sb.append("\""+TransactionController.getInstance().convertTimeStampToCointrackingReal(addPool1.blockTimeProperty.getValue())+"\"").append(exportSplitter);
                            sb.append("\"").append(addPool1.txIDProperty.getValue()).append("\"").append(exportSplitter);
                            sb.append("\"").append(addPool1.fiatValueProperty.getValue()).append("\"").append(exportSplitter);
                            sb.append("\"").append(addPool1.fiatValueProperty.getValue()).append("\"");
                            writer.write(sb.toString());
                            sb = null;

                            sb = new StringBuilder();

                            sb.append("\n");
                            sb.append("\"Trade\"").append(exportSplitter);
                            sb.append("\""+String.format(localeDecimal, "%.8f", addPool2.cryptoValueProperty.getValue())+"\"").append(exportSplitter);
                            sb.append("\""+addPool2.cryptoCurrencyProperty.getValue()+"\"").append(exportSplitter);
                            sb.append("\""+String.format(localeDecimal, "%.8f", (addPool.cryptoValueProperty.getValue() / 2)* -1)+"\"").append(exportSplitter);
                            sb.append("\""+TransactionController.getInstance().getPoolPairFromId(addPool.poolIDProperty.getValue())+"\"").append(exportSplitter);
                            sb.append("\"\"").append(exportSplitter);
                            sb.append("\"\"").append(exportSplitter);
                            sb.append("\"DeFiChain-Wallet\"").append(exportSplitter);
                            sb.append("\"\"").append(exportSplitter);
                            sb.append("\"Remove-Pool-Liquidity (" + TransactionController.getInstance().getPoolPairFromId(addPool.poolIDProperty.getValue()) + ")\"").append(exportSplitter);
                            sb.append(TransactionController.getInstance().convertTimeStampToCointrackingReal(addPool2.blockTimeProperty.getValue())).append(exportSplitter);
                            sb.append("\"").append(addPool2.txIDProperty.getValue()).append("\"").append(exportSplitter);
                            sb.append("\"").append(addPool2.fiatValueProperty.getValue()).append("\"").append(exportSplitter);
                            sb.append("\"").append(addPool2.fiatValueProperty.getValue()).append("\"");
                            writer.write(sb.toString());
                            sb = null;
                        }
                    }

                    if ((transaction.typeProperty.getValue().equals("receive") | transaction.typeProperty.getValue().equals("sent")) && !transaction.exportCointracking) {

                        double amount = 0;
                        int onlyOne = 0;
                        for (int i = transCounter; i < transactions.size(); i++) {
                            if (transactions.get(i).blockHeightProperty.getValue() > transaction.blockHeightProperty.getValue())
                                break;

                            if (transactions.get(i).txIDProperty.getValue().equals(transaction.txIDProperty.getValue())) {
                                amount = amount + transactions.get(i).cryptoValueProperty.getValue();
                                transaction.exportCointracking = true;
                                transactions.get(i).exportCointracking = true;
                                onlyOne++;
                            }
                        }

                        if (!transaction.cryptoValueProperty.getValue().equals(0.0) && !(amount >= -0.00000001 && amount <= 0.00000001)) {
                            if (amount > 0) {
                                sb = new StringBuilder();
                                sb.append("\n");
                                sb.append("\"Deposit\"").append(exportSplitter);
                                sb.append("\""+String.format(localeDecimal, "%.8f", amount)+"\"").append(exportSplitter);
                                sb.append("\""+transaction.cryptoCurrencyProperty.getValue()+"\"").append(exportSplitter);
                                sb.append("\"\"").append(exportSplitter);
                                sb.append("\"\"").append(exportSplitter);
                                sb.append("\"\"").append(exportSplitter);
                                sb.append("\"\"").append(exportSplitter);
                                sb.append("\"DeFiChain-Wallet\"").append(exportSplitter);
                                sb.append("\"\"").append(exportSplitter);
                                sb.append("\"\"").append(exportSplitter);
                                sb.append("\""+TransactionController.getInstance().convertTimeStampToCointrackingReal(transaction.blockTimeProperty.getValue())+"\"").append(exportSplitter);
                                sb.append("\"").append(transaction.txIDProperty.getValue()).append("\"").append(exportSplitter);
                                sb.append("\"\"").append(exportSplitter);
                                sb.append("\"\"");
                                writer.write(sb.toString());
                                sb = null;

                            } else {
                                sb = new StringBuilder();

                                sb.append("\n");
                                sb.append("\"Withdrawal\"").append(exportSplitter);
                                sb.append("\"\"").append(exportSplitter);
                                sb.append("\"\"").append(exportSplitter);
                                sb.append("\""+String.format(localeDecimal, "%.8f", amount * -1)+"\"").append(exportSplitter);
                                sb.append("\""+transaction.cryptoCurrencyProperty.getValue()+"\"").append(exportSplitter);
                                sb.append("\"\"").append(exportSplitter);
                                sb.append("\"\"").append(exportSplitter);
                                sb.append("\"DeFiChain-Wallet\"").append(exportSplitter);
                                sb.append("\"\"").append(exportSplitter);
                                sb.append("\"\"").append(exportSplitter);
                                sb.append("\""+TransactionController.getInstance().convertTimeStampToCointrackingReal(transaction.blockTimeProperty.getValue())+"\"").append(exportSplitter);
                                sb.append("\"").append(transaction.txIDProperty.getValue()).append("\"").append(exportSplitter);
                                sb.append("\"\"").append(exportSplitter);
                                sb.append("\"\"");
                                writer.write(sb.toString());
                                sb = null;
                            }
                        }
                    }

                    if (transaction.typeProperty.getValue().equals("AccountToAccount") && !transaction.exportCointracking) {
                        for (int i = transCounter; i < transactions.size(); i++) {
                            if (transactions.get(i).blockHeightProperty.getValue() > transaction.blockHeightProperty.getValue())
                                break;

                            if (transactions.get(i).txIDProperty.getValue().equals(transaction.txIDProperty.getValue()) && transactions.get(i).cryptoValueProperty.getValue().equals(-1 * transaction.cryptoValueProperty.getValue()) && !transactions.get(i).typeProperty.getValue().equals("sent")) {
                                transaction.exportCointracking = true;
                                transactions.get(i).exportCointracking = true;
                                break;
                            }
                        }

                        if (!transaction.exportCointracking && !transaction.cryptoValueProperty.getValue().equals(0.0)) {
                            sb = new StringBuilder();

                            sb.append("\n");
                            if (transaction.cryptoValueProperty.getValue() < 0) {
                                sb.append("\"Withdrawal\"").append(exportSplitter);
                                sb.append("\"\"").append(exportSplitter);
                                sb.append("\"\"").append(exportSplitter);
                                sb.append("\""+String.format(localeDecimal, "%.8f", transaction.cryptoValueProperty.getValue() * -1)+"\"").append(exportSplitter);
                                sb.append("\""+transaction.cryptoCurrencyProperty.getValue()+"\"").append(exportSplitter);
                            } else {
                                sb.append("\"Deposit\"").append(exportSplitter);
                                sb.append("\""+String.format(localeDecimal, "%.8f", transaction.cryptoValueProperty.getValue())+"\"").append(exportSplitter);
                                sb.append("\""+transaction.cryptoCurrencyProperty.getValue()+"\"").append(exportSplitter);
                                sb.append("\"\"").append(exportSplitter);
                                sb.append("\"\"").append(exportSplitter);
                            }

                            sb.append("\"\"").append(exportSplitter);
                            sb.append("\"\"").append(exportSplitter);
                            sb.append("\"DeFiChain-Wallet\"").append(exportSplitter);
                            sb.append("\"\"").append(exportSplitter);
                            sb.append("\"\"").append(exportSplitter);
                            sb.append("\"" +TransactionController.getInstance().convertTimeStampToCointrackingReal(transaction.blockTimeProperty.getValue())+"\"").append(exportSplitter);
                            sb.append("\"").append(transaction.txIDProperty.getValue()).append("\"").append(exportSplitter);
                            sb.append("\"\"").append(exportSplitter);
                            sb.append("\"\"");
                            writer.write(sb.toString());
                            sb = null;
                        }
                    }

                    if (transaction.typeProperty.getValue().equals("AnyAccountsToAccounts") && !transaction.exportCointracking) {

                        double amount = 0;
                        int onlyOne = 0;
                        for (int i = transCounter; i < transactions.size(); i++) {
                            if (transactions.get(i).blockHeightProperty.getValue() > transaction.blockHeightProperty.getValue())
                                break;

                            if (transactions.get(i).txIDProperty.getValue().equals(transaction.txIDProperty.getValue())) {
                                amount = amount + transactions.get(i).cryptoValueProperty.getValue();
                                transaction.exportCointracking = true;
                                transactions.get(i).exportCointracking = true;
                                onlyOne++;
                            }
                        }

                        if ((!transaction.exportCointracking || onlyOne == 1) && !transaction.cryptoValueProperty.getValue().equals(0.0) && !(amount >= -0.00000001 && amount <= 0.00000001)) {
                            sb = new StringBuilder();

                            sb.append("\n");
                            if (transaction.cryptoValueProperty.getValue() < 0) {
                                sb.append("\"Withdrawal\"").append(exportSplitter);
                                sb.append("\"\"").append(exportSplitter);
                                sb.append("\"\"").append(exportSplitter);
                                sb.append("\""+String.format(localeDecimal, "%.8f", transaction.cryptoValueProperty.getValue() * -1)+"\"").append(exportSplitter);
                                sb.append("\""+transaction.cryptoCurrencyProperty.getValue()+"\"").append(exportSplitter);
                            } else {
                                sb.append("\"Deposit\"").append(exportSplitter);
                                sb.append("\""+String.format(localeDecimal, "%.8f", transaction.cryptoValueProperty.getValue())+"\"").append(exportSplitter);
                                sb.append("\""+transaction.cryptoCurrencyProperty.getValue()+"\"").append(exportSplitter);
                                sb.append("\"\"").append(exportSplitter);
                                sb.append("\"\"").append(exportSplitter);
                            }

                            sb.append("\"\"").append(exportSplitter);
                            sb.append("\"\"").append(exportSplitter);
                            sb.append("\"DeFiChain-Wallet\"").append(exportSplitter);
                            sb.append("\"\"").append(exportSplitter);
                            sb.append("\"\"").append(exportSplitter);
                            sb.append("\""+TransactionController.getInstance().convertTimeStampToCointrackingReal(transaction.blockTimeProperty.getValue())+"\"").append(exportSplitter);
                            sb.append("\"").append(transaction.txIDProperty.getValue()).append("\"").append(exportSplitter);
                            sb.append("\"\"").append(exportSplitter);
                            sb.append("\"\"");
                            writer.write(sb.toString());
                            sb = null;
                        }


                    }
                }

                oldDate = newDate;
                transCounter++;
            }
        }


        for (
                HashMap.Entry<String, TransactionModel> entry : exportList.entrySet()) {

            sb = new StringBuilder();

            sb.append("\n");
            sb.append("\"" + Type2CointrackingType(entry.getValue().typeProperty.getValue()) + "\"").append(exportSplitter);
            sb.append("\""+String.format(localeDecimal, "%.8f", entry.getValue().cryptoValueProperty.getValue())+"\"").append(exportSplitter);
            sb.append("\""+entry.getValue().cryptoCurrencyProperty.getValue()+"\"").append(exportSplitter);
            sb.append("\"\"").append(exportSplitter);
            sb.append("\"\"").append(exportSplitter);
            sb.append("\"\"").append(exportSplitter);
            sb.append("\"\"").append(exportSplitter);
            sb.append("\"DeFiChain-Wallet\"").append(exportSplitter);
            sb.append("\"\"").append(exportSplitter);

            switch (filter) {
                case "Cumulate All":
                    sb.append("\""+"LM Interest Income"+"\"").append(exportSplitter);
                    break;
                case "Cumulate Rewards and Commisions":
                    sb.append("\""+"LM Interest Income (" + this.mainViewController.transactionController.getPoolPairFromId(entry.getValue().poolIDProperty.getValue())).append(")"+"\"").append(exportSplitter);
                    break;
                case "Cumulate Pool Pair":
                    sb.append("\""+"LM " + entry.getValue().typeProperty.getValue()+"\"").append(exportSplitter);
                    break;
                case "Cumulate None":
                    sb.append("\""+"LM " + entry.getValue().typeProperty.getValue() + " (" + this.mainViewController.transactionController.getPoolPairFromId(entry.getValue().poolIDProperty.getValue())).append(")"+"\"").append(exportSplitter);
                    break;
            }

            sb.append("\""+TransactionController.getInstance().convertTimeStampToCointracking(entry.getValue().blockTimeProperty.getValue())+"\"").append(exportSplitter);
            sb.append("\""+entry.getValue().cryptoCurrencyProperty.getValue() + TransactionController.getInstance().convertTimeStampWithoutTimeToString(entry.getValue().blockTimeProperty.getValue())+"\"").append(exportSplitter);
            sb.append("\"\"").append(exportSplitter);
            sb.append("\"\"");

            writer.write(sb.toString());
            sb = null;

        }

        writer.close();
        exportList.clear();
        SettingsController.getInstance().checkCointracking = true;
        return true;

    }

    public String Type2CointrackingType(String type) {
        switch (type) {
            case "Rewards":
            case "Commission":
                return "Interest Income";
            case "PoolSwap":
            case "AddPoolLiquidity":
            case "RemovePoolLiquidity":
                return "Trade";
            case "receive":
                return "Deposit";
            case "sent":
                return "Withdrawal";
            default:
                return "Interest Income";
        }
    }


    public boolean exportPoolPairToExcel(List<PoolPairModel> poolPairModelList, String exportPath, String
            source, MainView mainView) {
        try {
            PrintWriter writer = new PrintWriter(exportPath);
            StringBuilder sb = new StringBuilder();

            Locale localeDecimal = Locale.GERMAN;
            if (this.mainViewController.settingsController.selectedDecimal.getValue().equals(".")) {
                localeDecimal = Locale.US;
            }
            switch (mainView.tabPane.getSelectionModel().getSelectedItem().getId()) {
                case "Portfolio":
                    sb.append((mainView.plotTable.getColumns().get(0).getId() + "," + mainView.plotTable.getColumns().get(2).getId() + "," + mainView.plotTable.getColumns().get(2).getId() + "," + mainView.plotTable.getColumns().get(9).getId()).replace(",", this.mainViewController.settingsController.selectedSeperator.getValue())).append("\n");
                    for (PoolPairModel poolPairModel : poolPairModelList) {
                        sb.append(poolPairModel.getBlockTime().getValue()).append(this.mainViewController.settingsController.selectedSeperator.getValue());
                        sb.append(poolPairModel.getPoolPair().getValue()).append(this.mainViewController.settingsController.selectedSeperator.getValue());
                        sb.append(poolPairModel.getBalanceFiatValue()).append(this.mainViewController.settingsController.selectedSeperator.getValue());
                        sb.append("\n");
                    }
                    break;
                case "Overview":
                    sb.append((mainView.plotTable.getColumns().get(0).getId() + "," + mainView.plotTable.getColumns().get(1).getId() + "," + mainView.plotTable.getColumns().get(2).getId() + "," + mainView.plotTable.getColumns().get(3).getId() + "," + mainView.plotTable.getColumns().get(4).getId() + "," + mainView.plotTable.getColumns().get(5).getId() + "," + mainView.plotTable.getColumns().get(6).getId() + "," + mainView.plotTable.getColumns().get(7).getId() + "," + mainView.plotTable.getColumns().get(8).getId()).replace(",", this.mainViewController.settingsController.selectedSeperator.getValue())).append("\n");
                    for (PoolPairModel poolPairModel : poolPairModelList) {
                        sb.append(poolPairModel.getBlockTime().getValue()).append(this.mainViewController.settingsController.selectedSeperator.getValue());
                        sb.append(poolPairModel.getPoolPair().getValue()).append(this.mainViewController.settingsController.selectedSeperator.getValue());
                        sb.append(String.format(localeDecimal, "%.8f", poolPairModel.getCryptoValue1().getValue())).append(this.mainViewController.settingsController.selectedSeperator.getValue());
                        sb.append(String.format(localeDecimal, "%.8f", poolPairModel.getCryptoFiatValue1().getValue())).append(this.mainViewController.settingsController.selectedSeperator.getValue());
                        sb.append(String.format(localeDecimal, "%.8f", poolPairModel.getCryptoValue2().getValue())).append(this.mainViewController.settingsController.selectedSeperator.getValue());
                        sb.append(String.format(localeDecimal, "%.8f", poolPairModel.getCryptoFiatValue2().getValue())).append(this.mainViewController.settingsController.selectedSeperator.getValue());
                        sb.append(String.format(localeDecimal, "%.8f", poolPairModel.getcryptoCommission2Overviewvalue())).append(this.mainViewController.settingsController.selectedSeperator.getValue());
                        sb.append(String.format(localeDecimal, "%.8f", poolPairModel.getcryptoCommission2FiatOverviewvalue())).append(this.mainViewController.settingsController.selectedSeperator.getValue());
                        sb.append(String.format(localeDecimal, "%.8f", poolPairModel.getFiatValue().getValue())).append(this.mainViewController.settingsController.selectedSeperator.getValue());
                        sb.append("\n");
                    }
                    break;
                case "Commissions":
                    sb.append((mainView.plotTable.getColumns().get(0).getId() + "," + mainView.plotTable.getColumns().get(1).getId() + "," + mainView.plotTable.getColumns().get(2).getId() + "," + mainView.plotTable.getColumns().get(3).getId() + "," + mainView.plotTable.getColumns().get(4).getId() + "," + mainView.plotTable.getColumns().get(5).getId() + "," + mainView.plotTable.getColumns().get(8).getId()).replace(",", this.mainViewController.settingsController.selectedSeperator.getValue())).append("\n");
                    for (PoolPairModel poolPairModel : poolPairModelList) {
                        sb.append(poolPairModel.getBlockTime().getValue()).append(this.mainViewController.settingsController.selectedSeperator.getValue());
                        sb.append(poolPairModel.getPoolPair().getValue()).append(this.mainViewController.settingsController.selectedSeperator.getValue());
                        sb.append(String.format(localeDecimal, "%.8f", poolPairModel.getCryptoValue1().getValue())).append(this.mainViewController.settingsController.selectedSeperator.getValue());
                        sb.append(String.format(localeDecimal, "%.8f", poolPairModel.getCryptoFiatValue1().getValue())).append(this.mainViewController.settingsController.selectedSeperator.getValue());
                        sb.append(String.format(localeDecimal, "%.8f", poolPairModel.getCryptoValue2().getValue())).append(this.mainViewController.settingsController.selectedSeperator.getValue());
                        sb.append(String.format(localeDecimal, "%.8f", poolPairModel.getCryptoFiatValue2().getValue())).append(this.mainViewController.settingsController.selectedSeperator.getValue());
                        sb.append(String.format(localeDecimal, "%.8f", poolPairModel.getFiatValue().getValue())).append(this.mainViewController.settingsController.selectedSeperator.getValue());
                        sb.append("\n");
                    }
                    break;
                case "Rewards":
                    sb.append((mainView.plotTable.getColumns().get(0).getId() + "," + mainView.plotTable.getColumns().get(1).getId() + "," + mainView.plotTable.getColumns().get(2).getId() + "," + mainView.plotTable.getColumns().get(3).getId()).replace(",", this.mainViewController.settingsController.selectedSeperator.getValue())).append("\n");
                    for (PoolPairModel poolPairModel : poolPairModelList) {
                        sb.append(poolPairModel.getBlockTime().getValue()).append(this.mainViewController.settingsController.selectedSeperator.getValue());
                        sb.append(poolPairModel.getPoolPair().getValue()).append(this.mainViewController.settingsController.selectedSeperator.getValue());
                        sb.append(String.format(localeDecimal, "%.8f", poolPairModel.getCryptoValue1().getValue())).append(this.mainViewController.settingsController.selectedSeperator.getValue());
                        sb.append(String.format(localeDecimal, "%.8f", poolPairModel.getCryptoFiatValue1().getValue())).append(this.mainViewController.settingsController.selectedSeperator.getValue());
                        sb.append("\n");
                    }
                    break;
                default:
                    break;
            }
            writer.write(sb.toString());
            writer.close();
            return true;
        } catch (FileNotFoundException e) {
            SettingsController.getInstance().logger.warning("Exception occured: " + e.toString());
            return false;
        }
    }

    public static String getIdFromPoolPair(String poolID) {
        String pool;
        switch (poolID) {
            case "DFI":
                pool = "0";
                break;
            case "ETH":
                pool = "1";
                break;
            case "BTC":
                pool = "2";
                break;
            case "USDT":
                pool = "3";
                break;
            case "ETH-DFI":
                pool = "4";
                break;
            case "BTC-DFI":
                pool = "5";
                break;
            case "USDT-DFI":
                pool = "6";
                break;
            case "DOGE":
                pool = "7";
                break;
            case "DOGE-DFI":
                pool = "8";
                break;
            case "LTC":
                pool = "9";
                break;
            case "LTC-DFI":
                pool = "10";
                break;
            case "BCH":
                pool = "11";
                break;
            case "BCH-DFI":
                pool = "12";
                break;
            default:
                pool = "-";
                break;
        }
        return pool;
    }


}