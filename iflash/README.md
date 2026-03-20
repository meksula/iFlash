# iflash
iflash is a fast, efficient, real-time response app for stock market simulation.

![iflash system logo](iflash-logo.png)


<h2>iflash API description</h2>

<h5>Get all financial instrument list</h5>
METHOD: `GET`    
URI: `/api/v1/instrument`
> Use this endpoint to retrieve the list of available financial instruments on the iFlash exchange.

### Response Body Fields
| Field           | Type   | Possible values | Description                                                                 |
|-----------------|--------|-----------------|-----------------------------------------------------------------------------|
| `ticker`        | String | Any valid ticker | The ticker symbol of the financial instrument.                             |
| `currentPrice`  | Float  | Any positive number | The current price of the financial instrument.                             |

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

<h5>Order Book view</h5>
METHOD: `GET`    
URI: `/api/v1/orderbook/{ticker}?page=0&size=20&orderBy=DESC&orderDirection=BID`
> Use this endpoint to view paginated result of current order book state.

### URL Parameters
| Parameter       | Type   | Possible values | Description                                                                 |
|-----------------|--------|-----------------|-----------------------------------------------------------------------------|
| `ticker`        | String | Any valid ticker | The ticker symbol of the financial instrument.                             |
| `page`          | Integer| Any non-negative integer | The page number for pagination.                                             |
| `size`          | Integer| Any positive integer | The number of results per page.                                             |
| `orderBy`       | String | `ASC`, `DESC`   | The order of sorting.                                                       |
| `orderDirection`| String | `BID`, `ASK`    | The direction of the order.                                                 |

### Response Body Fields
| Field           | Type   | Possible values | Description                                                                 |
|-----------------|--------|-----------------|-----------------------------------------------------------------------------|
| `responseZonedDateTime`| String | ISO 8601 datetime | The timestamp of the response.                                              |
| `ticker`        | String | Any valid ticker | The ticker symbol of the financial instrument.                             |
| `asks`          | Object | N/A             | Contains the list of ask orders and pagination details.                     |

Example response body:
```
{
    "responseZonedDateTime": "2025-10-22T23:04:44.185544+02:00",
    "ticker": "NVDA",
    "asks": {
        "elements": [
            {
                "orderCreationDate": "2025-10-22T23:04:26.224416+02:00",
                "price": 185.10,
                "volume": 10
            },
            {
                "orderCreationDate": "2025-10-22T23:04:20.746634+02:00",
                "price": 185.00,
                "volume": 10
            }
        ],
        "pagination": {
            "page": 0,
            "size": 20,
            "orderBy": "DESC"
        }
    }
}
```

<h5>Current quotation</h5>
METHOD: `GET`    
URI: `/api/v1/quotation/{ticker}/price`
> Use this endpoint to fetch current quotation for any supported ticker.

### URL Parameters
| Parameter       | Type   | Possible values | Description                                                                 |
|-----------------|--------|-----------------|-----------------------------------------------------------------------------|
| `ticker`        | String | Any valid ticker | The ticker symbol of the financial instrument.                             |

### Response Body Fields
| Field           | Type   | Possible values | Description                                                                 |
|-----------------|--------|-----------------|-----------------------------------------------------------------------------|
| `responseZonedDateTime`| String | ISO 8601 datetime | The timestamp of the response.                                              |
| `quoteTimestamp`| Long   | Any positive number | The timestamp of the quote.                                                 |
| `ticker`        | String | Any valid ticker | The ticker symbol of the financial instrument.                             |
| `price`         | Float  | Any positive number | The current price of the financial instrument.                              |

Example response body:
```
{
    "responseZonedDateTime": "2025-10-22T21:45:25.828591+02:00",
    "quoteTimestamp": 1761162295020,
    "ticker": "NVDA",
    "price": 184.57
}
```

<h5>Quotation history</h5>
METHOD: `GET`    
URI: `/api/v1/quotation/{ticker}/{amount}/{order}`
> Use this endpoint to fetch historical quotations for any supported ticker.

### URL Parameters
| Parameter       | Type   | Possible values | Description                                                                 |
|-----------------|--------|-----------------|-----------------------------------------------------------------------------|
| `ticker`        | String | Any valid ticker | The ticker symbol of the financial instrument.                             |
| `amount`        | Integer| Any positive integer | The number of historical quotations to fetch.                               |
| `order`         | String | `ASC`, `DESC`   | The order of sorting.                                                       |

### Response Body Fields
| Field           | Type   | Possible values | Description                                                                 |
|-----------------|--------|-----------------|-----------------------------------------------------------------------------|
| `responseZonedDateTime`| String | ISO 8601 datetime | The timestamp of the response.                                              |
| `ticker`        | String | Any valid ticker | The ticker symbol of the financial instrument.                             |
| `quotations`    | Array  | N/A             | List of historical quotations.                                              |

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

<h5>Make order</h5>
METHOD: `POST`    
URI: `/api/v1/trade/order`
> Use this endpoint to make order BID or ASK type

### Request Body Fields
| Field           | Type   | Possible values | Description                                                                 |
|-----------------|--------|-----------------|-----------------------------------------------------------------------------|
| `orderDirection`| String | `BID`, `ASK`    | Direction of the order. Possible values: `BID` (sell), `ASK` (buy).         |
| `orderType`     | String | `MARKET`, `LIMIT` | Type of the order. Possible values: `MARKET` (immediate), `LIMIT` (delayed). |
| `ticker`        | String | Any valid ticker | The ticker symbol of the financial instrument.                              |
| `volume`        | Integer| Any positive integer | The number of shares to trade.                                              |
| `price`         | Float  | Any positive number | (Optional) The price per share for LIMIT orders.                            |

### Response Body Fields
| Field           | Type   | Possible values | Description                                                                 |
|-----------------|--------|-----------------|-----------------------------------------------------------------------------|
| `orderDirection`| String | `BID`, `ASK`    | Direction of the order. Reflects the request value.                        |
| `orderType`     | String | `MARKET`, `LIMIT` | Type of the order. Reflects the request value.                              |
| `ticker`        | String | Any valid ticker | The ticker symbol of the financial instrument.                             |
| `price`         | Float  | Any positive number | The price per share for the transaction.                                   |
| `volume`        | Integer| Any positive integer | The number of shares traded.                                               |
| `transactions`  | Array  | N/A             | List of transactions executed for the order.                               |

### URL Parameters
- None

Buy order request body example:
```
{
  "orderDirection": "ASK",
  "orderType": "MARKET",
  "ticker": "NVDA",
  "volume": 7
}
```
Buy order response body:
```
{
    "orderDirection": "ASK",
    "orderType": "MARKET",
    "ticker": "NVDA",
    "price": null,
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

Sell order request body example:
```
{
  "orderDirection": "BID",
  "orderType": "MARKET",
  "ticker": "NVDA",
  "price": "185.10",
  "volume": 10
}
```

Sell order response body:
```
{
    "orderDirection": "BID",
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