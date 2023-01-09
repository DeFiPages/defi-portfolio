import sys
import yfinance as yf
from datetime import date
import pandas
import numpy
import os
import pathlib
import platform
import requests
import pandas

if __name__ == '__main__':

    if platform.system() == 'Linux' or platform.system() == 'Windows':
        pathPortfolioData = sys.argv[1]
    else:
        pathPortfolioData = sys.argv[0]
        pathPortfolioData = pathPortfolioData.replace("/StockTokenPrices", "")  # match mac os path
    print("PATH: " + pathPortfolioData)
   # pathPortfolioData = os.environ.get("APPDATA") + '\\defi-portfolio'

    today = date.today()
    strDate = today.strftime("%Y-%m-%d")
    strStartDate = '2021-11-01'

    Tokens = []
   # Tokens = ['TSLA', 'GME', 'GOOGL', 'BABA', 'PLTR', 'AAPL', 'SPY', 'QQQ', 'PDBC', 'VNQ', 'ARKK', 'GLD', 'URTH', 'TLT',
   #           'SLV','COIN','AMZN','NVDA','EEM','INTC','DIS','MSFT','NFLX','VOO','MSTR','FB','MCHI','UNG','CS','PYPL','PPLT','XLRE','BRK-B',
   #           'XLE','DAX','TAN','USO','PDBC','GS','XOM','URA','VNQ','ADDYY','KO','PG','SAP','ARKX','VBK','UL','GOVT','WMT','JNJ']
    r = requests.get("https://api.defichain.io/v1/listtokens")
    r_dictionary = r.json()
    r_dictionary = pandas.DataFrame.from_dict(r_dictionary, orient='index')
    for iToken in range(0, r_dictionary.__len__()):
        print(iToken)
        if r_dictionary.iloc[iToken]['isDAT'] == True and r_dictionary.iloc[iToken]['mintable'] == True and r_dictionary.iloc[iToken]['name'].startswith('d'):
            Tokens.append(r_dictionary.iloc[iToken]['symbol'])
   # rename Fb to Meta
    if Tokens.__contains__('FB'):
        Tokens.remove('FB')
        Tokens.append('META')
    if Tokens.__contains__('BRK.B'):
        Tokens.remove('BRK.B')
        Tokens.append('BRK-B')

    resultUSD = pandas.DataFrame()

    for token in Tokens:
        currentToken = yf.download(token, strStartDate, strDate)
        currentToken = currentToken.drop(columns=['Open', 'High', 'Close', 'Adj Close', 'Volume'])
        currentToken = currentToken.rename(columns={"Low": token + "USD"})

        if resultUSD.__len__() == 0:
            resultUSD = pandas.DataFrame(currentToken)
        else:
            resultUSD = pandas.concat([resultUSD, currentToken], axis=1)

    resultUSD = resultUSD.round(2)
    result = resultUSD

#    EURUSD = yf.download('EURUSD=X', strStartDate, strDate)
#    EURUSD = EURUSD.drop(columns=['Open', 'High', 'Close', 'Adj Close', 'Volume'])

#    resultEUR = resultUSD / EURUSD['Low']

    result['Date'] = result.index

    first_column = result.pop('Date')
    result.insert(0, 'Date', first_column)


    index = pandas.DatetimeIndex(result['Date'])
    index = index.astype(numpy.int64).to_series() / 1000000000
    index = index.reset_index(drop="True")
    result = result.reset_index(drop="True")
    result['Date'] = index.astype(int)

    result.to_csv(pathPortfolioData + '/stockTockenPrices.portfolio', mode='w', header=True, sep=';', index=False)

    TokensDf = pandas.DataFrame(Tokens)
    TokensDf[0] = TokensDf[0].astype(str) +'-DUSD';
    TokensDf.loc[TokensDf.index.max() + 1] = ['BTC-DFI']
    TokensDf.loc[TokensDf.index.max() + 1] = ['ETH-DFI']
    TokensDf.loc[TokensDf.index.max() + 1] = ['USDT-DFI']
    TokensDf.loc[TokensDf.index.max() + 1] = ['LTC-DFI']
    TokensDf.loc[TokensDf.index.max() + 1] = ['BCH-DFI']
    TokensDf.loc[TokensDf.index.max() + 1] = ['DOGE-DFI']
    TokensDf.loc[TokensDf.index.max() + 1] = ['USDC-DFI']
    TokensDf.loc[TokensDf.index.max() + 1] = ['DUSD-DFI']


    TokensDf.to_csv(pathPortfolioData + '/stockTockens.portfolio', mode='w', header=False, sep=';', index=False)
    os.remove(pathPortfolioData + '/StockPricesPythonUpdate.portfolio')