To build and run the test, follow the instructions below:

1. Navigate to the `batm_public` directory:
    ```
    cd batm_public
    ```

2. Build the project and install the server extensions test:
    ```
    ./gradlew build && ./gradlew :server_extensions_test:install
    ```

3. List the wallets by running the following command:
    ```
    ./server_extensions_test/build/install/server_extensions_test/bin/server_extensions_test -j ./server_extensions_extra/build/libs/batm_server_extensions_extra.jar -a list-wallets
    ```

    The result will display the following information:
    ```
    -n=infuraERC20_APE_18_0x4d224452801aced8b2f0aebe155379bb5d594381 - Infura.io wallet - APE(APE)  -p=projectId:mnemonic:gaslimit:gasPriceMultiplier
    ```

4. Get the wallet balance by running the following command:
    ```
    ./server_extensions_test/build/install/server_extensions_test/bin/server_extensions_test -j ./server_extensions_extra/build/libs/batm_server_extensions_extra.jar -a get-wbalance -n infuraERC20_APE_18_0x4d224452801aced8b2f0aebe155379bb5d594381 -p <wallet-secret-and-parameters>
    ```

    The result will display the following information:
    ```
    CryptoAddress = 0x0947d8df6cdc3ef594919d003d98bea3174e6bb2
    Balance: 0 APE
    ```

5. List the exchanges by running the following command:
    ```
    ./server_extensions_test/build/install/server_extensions_test/bin/server_extensions_test -j ./server_extensions_extra/build/libs/batm_server_extensions_extra.jar -a list-exchanges
    ```

    The result will display the following information:
    ```
    -n=binancecom - Binance.com(BAT,BCH,BNB,BTC,CLOAK,DASH,DOGE,ETH,GRS,LSK,LTC,NANO,NULS,PAXG,REP,SYS,TRX,USDS,USDT,VIA,XMR,XRP,XZC,APE)  -p=apikey:secretkey:fiatcurrency
    ```

6. Get the exchange balance by running the following command:
    ```
    ./server_extensions_test/build/install/server_extensions_test/bin/server_extensions_test -j ./server_extensions_extra/build/libs/batm_server_extensions_extra.jar -a get-ebalance -n binancecom -p <exchange-secret-and-parameters>
    ```

    The result will display the following information:
    ```
    Crypto Balance: 0 APE
    Deposit Address: 0x1974e35c283457600f2c2dead8fbe935c8b44c44
    ```

Remember to replace <exchange-secret-and-parameters> with the appropriate values provided via direct message.