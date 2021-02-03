package com.generalbytes.batm.server.extensions.extra.bitcoin.wallets.cryptx.v2;

import com.generalbytes.batm.common.currencies.CryptoCurrency;
import com.generalbytes.batm.server.extensions.Converters;
import com.generalbytes.batm.server.extensions.IWallet;
import com.generalbytes.batm.server.extensions.extra.bitcoin.wallets.cryptx.v2.dto.Balance;
import com.generalbytes.batm.server.extensions.extra.bitcoin.wallets.cryptx.v2.dto.CryptXException;
import com.generalbytes.batm.server.extensions.extra.bitcoin.wallets.cryptx.v2.dto.CryptXSendTransactionRequest;
import com.generalbytes.batm.server.extensions.util.net.CompatSSLSocketFactory;
import okhttp3.HttpUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import si.mazi.rescu.ClientConfig;
import si.mazi.rescu.HttpStatusIOException;
import si.mazi.rescu.RestProxyFactory;

import javax.net.ssl.SSLContext;
import javax.ws.rs.HeaderParam;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.generalbytes.batm.server.extensions.extra.bitcoin.wallets.cryptx.v2.ICryptXAPI.*;

public class CryptXWallet implements IWallet {

    private static final Logger log = LoggerFactory.getLogger(CryptXWallet.class);

    protected final ICryptXAPI api;
    protected String walletId;
    protected String url;
    protected static final Integer readTimeout = 90 * 1000;
    private int priority;

    public CryptXWallet(String scheme, String host, int port, String token, String walletId, String priority) {
        this.walletId = walletId;
        this.url = new HttpUrl.Builder().scheme(scheme).host(host).port(port).build().toString();

        if (priority == null) {
            this.priority = 2;
        } else if (PRIORITY_LOW.equalsIgnoreCase(priority.trim())) {
            this.priority = 24;
        } else if (PRIORITY_MEDIUM.equalsIgnoreCase(priority.trim())) {
            this.priority = 8;
        } else if (PRIORITY_HIGH.equalsIgnoreCase(priority.trim())) {
            this.priority = 2;
        } else {
            this.priority = 2;
        }

        ClientConfig config = new ClientConfig();
        config.setHttpReadTimeout(readTimeout);

        config.addDefaultParam(HeaderParam.class, "Authorization", "Bearer " + token);

        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, null, null);
            final CompatSSLSocketFactory socketFactory = new CompatSSLSocketFactory(sslContext.getSocketFactory());
            config.setSslSocketFactory(socketFactory);
            config.setIgnoreHttpErrorCodes(true);
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            log.error("", e);
        }

        api = RestProxyFactory.createProxy(ICryptXAPI.class, this.url, config);
    }


    @Override
    public String sendCoins(String destinationAddress, BigDecimal amount, String cryptoCurrency, String description) {
        CryptXSendTransactionRequest sendTransactionRequest = new CryptXSendTransactionRequest(
            destinationAddress,
            toMinorUnit(cryptoCurrency, amount),
            description,
            priority != 0 ? priority : null
        );
        try {
            Map<String, Object> response = api.sendTransaction(cryptoCurrency.toLowerCase(), this.walletId, sendTransactionRequest);
            checkForSuccess(response);
            return getTxidFromSendTransactionResponse(response);
        } catch (HttpStatusIOException hse) {
            log.debug("send coins error - HttpStatusIOException, error message: {}, HTTP code: {}, HTTP content: {}", hse.getMessage(), hse.getHttpStatusCode(), hse.getHttpBody());
        } catch (CryptXException e) {
            log.debug("send coins error message: {}", e.getErrorMessage());
        } catch (Exception e) {
            log.error("Error", e);
        }
        return null;
    }

    @Override
    public String getCryptoAddress(String cryptoCurrency) {
        if (cryptoCurrency == null) {
            cryptoCurrency = getPreferredCryptoCurrency();
        }
        if (!getCryptoCurrencies().contains(cryptoCurrency)) {
            return null;
        }
        cryptoCurrency = cryptoCurrency.toLowerCase();
        try {
            String apiCryptocurrency = getAPICryptocurrency(cryptoCurrency);
            final Map<String, Object> wallet = api.getWallet(apiCryptocurrency, this.walletId, false);

            checkForSuccess(wallet);

            if (wallet == null || wallet.isEmpty()) {
                return null;
            }

            Object defaultAddress = wallet.get("defaultAddress");
            if (defaultAddress == null || !(defaultAddress instanceof String)) {
                return null;
            }

            return (String) defaultAddress;
        } catch (HttpStatusIOException hse) {
            log.debug("getCryptoAddress error: {}", hse.getHttpBody());
        } catch (CryptXException e) {
            log.debug("getCryptoAddress error: {}", e.getErrorMessage());
        } catch (Exception e) {
            log.error("", e);
        }

        return null;
    }

    @Override
    public Set<String> getCryptoCurrencies() {
        Set<String> coins = new HashSet<>();
        coins.add(CryptoCurrency.BTC.getCode());
        coins.add(CryptoCurrency.LTC.getCode());
        coins.add(CryptoCurrency.BCH.getCode());
        coins.add(CryptoCurrency.ETH.getCode());
        coins.add(CryptoCurrency.USDT.getCode());

        coins.add(CryptoCurrency.TBTC.getCode());
        coins.add(CryptoCurrency.TLTC.getCode());
        coins.add(CryptoCurrency.TBCH.getCode());
        coins.add(CryptoCurrency.TETH.getCode());
        return coins;
    }

    @Override
    public String getPreferredCryptoCurrency() {
        return CryptoCurrency.BTC.getCode();
    }

    @Override
    public BigDecimal getCryptoBalance(String cryptoCurrency) {
        if (cryptoCurrency == null) {
            cryptoCurrency = getPreferredCryptoCurrency();
        }
        if (!getCryptoCurrencies().contains(cryptoCurrency)) {
            return null;
        }
        cryptoCurrency = cryptoCurrency.toLowerCase();

        try {
            final Balance balance = api.getWalletBalance(cryptoCurrency, this.walletId, false);
            if (balance == null) {
                return null;
            }

            BigInteger spendableBalance = balance.getSpendableBalance();

            return toMajorUnit(cryptoCurrency, spendableBalance.toString());
        } catch (CryptXException e) {
            log.debug("getCryptoBalance error: {}", e.getErrorMessage());
        } catch (Exception e) {
            log.error("getCryptoBalance error", e);
        }

        return null;
    }

    public String getUrl() {
        return this.url;
    }

    protected void checkForSuccess(Map<String, Object> response) throws CryptXException {
        Object errorKey = response.get("errorKey");
        if (errorKey != null) {
            throw new CryptXException((String) response.get("errorMessage"), (String) response.get("errorKey"));
        }
    }

    public String getAPICryptocurrency(String cryptoCurrency) {
        if (cryptoCurrency.equalsIgnoreCase(CryptoCurrency.USDT.getCode())) {
            return CryptoCurrency.ETH.getCode();
        }
        return cryptoCurrency;
    }

    private String toMinorUnit(String cryptoCurrency, BigDecimal amount) {
        try {
            switch (CryptoCurrency.valueOfCode(cryptoCurrency)) {
                case TBTC:
                case BTC:
                    return amount.multiply(Converters.BTC).toBigInteger().toString();
                case TLTC:
                case LTC:
                    return amount.multiply(Converters.LTC).toBigInteger().toString();
                case TBCH:
                case BCH:
                    return amount.multiply(Converters.BCH).toBigInteger().toString();
                case TETH:
                case ETH:
                    return amount.multiply(Converters.ETH).toBigInteger().toString();
                case USDT:
                    return amount.multiply(Converters.USDT).toBigInteger().toString();
                default:
                    return amount.toBigInteger().toString();
            }
        } catch (IllegalArgumentException e) {
            return amount.toBigInteger().toString();
        }
    }

    private BigDecimal toMajorUnit(String cryptoCurrency, String amount) {

        try {
            BigInteger bigIntegerAmount = new BigInteger(amount);
            switch (CryptoCurrency.valueOfCode(cryptoCurrency)) {
                case TBTC:
                case BTC:
                    return new BigDecimal(bigIntegerAmount).divide(Converters.BTC);
                case TLTC:
                case LTC:
                    return new BigDecimal(bigIntegerAmount).divide(Converters.LTC);
                case TBCH:
                case BCH:
                    return new BigDecimal(bigIntegerAmount).divide(Converters.BCH);
                case TETH:
                case ETH:
                    return new BigDecimal(bigIntegerAmount).divide(Converters.ETH);
                case USDT:
                    return new BigDecimal(bigIntegerAmount).divide(Converters.USDT);
                default:
                    return new BigDecimal(bigIntegerAmount);
            }
        } catch (IllegalArgumentException e) {
            return new BigDecimal(amount);
        }
    }

    private String getTxidFromSendTransactionResponse(Map<String, Object> response) {
        if (response != null && response.get("txid") instanceof String) {
            return (String) response.get("txid");
        }
        return null;
    }


}
