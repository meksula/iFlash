# iflash

iflash is a fast, efficient, real-time response app for stock market simulation.

![iflash system logo](iflash-logo.png)

<h2>iflash API description</h2>

<h3>Get all financial instrument list</h3>
METHOD: `GET`    
URI: `/api/v1/instrument`
> Use this endpoint to retrieve the list of available financial instruments on the iFlash exchange.

### Response Body Fields

| Field          | Type   | Possible values     | Description                                    |
|----------------|--------|---------------------|------------------------------------------------|
| `ticker`       | String | Any valid ticker    | The ticker symbol of the financial instrument. |
| `currentPrice` | Float  | Any positive number | The current price of the financial instrument. |

Example response body:

```
[
    {
        "ticker": "AAPL",
        "currentPrice": 245.27
    },
    {
        "ticker": "AMT",
        "currentPrice": 228.40
    },
    {
        "ticker": "AMZN",
        "currentPrice": 216.37
    }
]    
```

<h3>Order Book view</h3>
METHOD: `GET`    
URI: `/api/v1/orderbook/{ticker}?page=0&size=20&orderBy=DESC&orderDirection=BID`
> Use this endpoint to retrieve a paginated view of the current order book state.

### URL Parameters

| Parameter        | Type    | Possible values          | Description                                    |
|------------------|---------|--------------------------|------------------------------------------------|
| `ticker`         | String  | Any valid ticker         | The ticker symbol of the financial instrument. |
| `page`           | Integer | Any non-negative integer | The page number for pagination.                |
| `size`           | Integer | Any positive integer     | The number of results per page.                |
| `orderBy`        | String  | `ASC`, `DESC`            | The order of sorting.                          |
| `orderDirection` | String  | `BID`, `ASK`             | The direction of the order.                    |

### Response Body Fields

| Field                   | Type   | Possible values   | Description                                                      |
|-------------------------|--------|-------------------|------------------------------------------------------------------|
| `responseZonedDateTime` | String | ISO 8601 datetime | The timestamp of the response.                                   |
| `ticker`                | String | Any valid ticker  | The ticker symbol of the financial instrument.                   |
| `orderDirection`        | String | `BID`, `ASK`      | The order side returned in the response.                         |
| `data`                  | Object | N/A               | Contains the list of order book entries and pagination details.  |

Example response body:

```
{
    "responseZonedDateTime": "2026-03-20T23:20:30.945503+01:00",
    "ticker": "NVDA",
    "orderDirection": "BID",
    "data": {
        "elements": [],
        "pagination": {
            "page": 0,
            "size": 20,
            "orderBy": "DESC"
        }
    }
}
```

<h3>Current quotation</h3>
METHOD: `GET`    
URI: `/api/v1/quotation/{ticker}/price`
> Use this endpoint to fetch current quotation for any supported ticker.

### URL Parameters

| Parameter | Type   | Possible values  | Description                                    |
|-----------|--------|------------------|------------------------------------------------|
| `ticker`  | String | Any valid ticker | The ticker symbol of the financial instrument. |

### Response Body Fields

| Field                   | Type   | Possible values     | Description                                    |
|-------------------------|--------|---------------------|------------------------------------------------|
| `responseZonedDateTime` | String | ISO 8601 datetime   | The timestamp of the response.                 |
| `quoteTimestamp`        | Long   | Any positive number | The timestamp of the quote.                    |
| `ticker`                | String | Any valid ticker    | The ticker symbol of the financial instrument. |
| `price`                 | Float  | Any positive number | The current price of the financial instrument. |

Example response body:

```
{
    "responseZonedDateTime": "2025-10-22T21:45:25.828591+02:00",
    "quoteTimestamp": 1761162295020,
    "ticker": "NVDA",
    "price": 184.57
}
```

<h3>Quotation history</h3>
METHOD: `GET`    
URI: `/api/v1/quotation/{ticker}/{amount}/{order}`
> Use this endpoint to fetch historical quotations for any supported ticker.

### URL Parameters

| Parameter | Type    | Possible values      | Description                                    |
|-----------|---------|----------------------|------------------------------------------------|
| `ticker`  | String  | Any valid ticker     | The ticker symbol of the financial instrument. |
| `amount`  | Integer | Any positive integer | The number of historical quotations to fetch.  |
| `order`   | String  | `ASC`, `DESC`        | The order of sorting.                          |

### Response Body Fields

| Field                   | Type   | Possible values   | Description                                    |
|-------------------------|--------|-------------------|------------------------------------------------|
| `responseZonedDateTime` | String | ISO 8601 datetime | The timestamp of the response.                 |
| `ticker`                | String | Any valid ticker  | The ticker symbol of the financial instrument. |
| `quotations`            | Array  | N/A               | List of historical quotations.                 |

Example response body:

```
{
    "responseZonedDateTime": "2025-10-22T23:26:22.842707+02:00",
    "ticker": "NVDA",
    "quotations": [
        {
            "quoteTimestamp": 1761168378908,
            "price": 185.10
        },
        {
            "quoteTimestamp": 1761168377694,
            "price": 185.11
        },
        {
            "quoteTimestamp": 1761168361807,
            "price": 183.16
        }
    ]
}
```

<br>
<br>

<h3>Place order</h3>
METHOD: `POST`    
URI: `/api/v1/trade/order`
> Use this endpoint to place a BID or ASK order.

### Request Body Fields

| Field            | Type    | Possible values      | Description                                                                  |
|------------------|---------|----------------------|------------------------------------------------------------------------------|
| `orderDirection` | String  | `BID`, `ASK`         | Direction of the order. Possible values: `BID` (buy), `ASK` (sell).          |
| `orderType`      | String  | `MARKET`, `LIMIT`    | Type of the order. Possible values: `MARKET` (immediate), `LIMIT` (delayed). |
| `ticker`         | String  | Any valid ticker     | The ticker symbol of the financial instrument.                               |
| `volume`         | Integer | Any positive integer | The number of shares to trade.                                               |
| `price`          | Float   | Any positive number  | (Optional) The price per share for LIMIT orders.                             |

### Response Body Fields

| Field            | Type    | Possible values      | Description                                         |
|------------------|---------|----------------------|-----------------------------------------------------|
| `orderDirection` | String  | `BID`, `ASK`         | Direction of the order. Reflects the request value. |
| `orderType`      | String  | `MARKET`, `LIMIT`    | Type of the order. Reflects the request value.      |
| `ticker`         | String  | Any valid ticker     | The ticker symbol of the financial instrument.      |
| `price`          | Float   | Any positive number  | The average execution price of the order.           |
| `volume`         | Integer | Any positive integer | The number of shares traded.                        |
| `transactions`   | Array   | N/A                  | List of transactions executed for the order.        |

### URL Parameters

- None

Buy order request body example:

```
{
  "orderDirection": "BID",
  "orderType": "MARKET",
  "ticker": "NVDA",
  "volume": 7
}
```

Buy order response body:

```
{
    "orderDirection": "BID",
    "orderType": "MARKET",
    "ticker": "NVDA",
    "price": 184.50,
    "volume": 7,
    "transactions": [
        {
            "volume": 3,
            "price": 184.00
        },
        {
            "volume": 4,
            "price": 185.00
        }
    ]
}
```

Sell order Market type request body example:

```
{
  "orderDirection": "ASK",
  "orderType": "MARKET",
  "ticker": "NVDA",
  "volume": 10
}
```
Sell order Market type response body:

```
{
    "orderDirection": "ASK",
    "orderType": "MARKET",
    "ticker": "NVDA",
    "price": 185.10,
    "volume": 10,
    "transactions": [
        {
            "volume": 10,
            "price": 185.10
        }
    ]
}
```

Sell order Limit type request body example:

```
{
  "orderDirection": "ASK",
  "orderType": "LIMIT",
  "price": 185.10,
  "ticker": "NVDA",
  "volume": 10
}
```
Sell order Limit type response body:

```
{
    "orderDirection": "ASK",
    "orderType": "LIMIT",
    "ticker": "NVDA",
    "price": 185.10,
    "volume": 10,
    "transactions": [
        {
            "volume": 10,
            "price": 185.10
        }
    ]
}
```