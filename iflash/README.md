# iflash
iflash is a fast, efficient, real-time response app for stock market simulation.

![iflash system logo](iflash-logo.png)


<h2>iflash API description</h2>

<br>
<br>

<h5>Get all financial instrument list</h5>
METHOD: `GET`  
URI: `/api/v1/instrument`
> Use this endpoint to retrieve the list of available financial instruments on the iFlash exchange.

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

<br>
<br>

<h5>Order Book view</h5>
METHOD: `GET`    
URI: `/api/v1/orderbook/NVDA?page=0&size=20&orderBy=DESC`
> Use this endpoint to view paginated result of current order book state. Only asks positions are supported only for now.

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

<br>
<br>

<h5>Current quotation</h5>
METHOD: `GET`    
URI: `/api/v1/quotation/NVDA/price`
> Use this endpoint to fetch current quotation for any supported ticker

Example response body:
```
{
    "responseZonedDateTime": "2025-10-22T21:45:25.828591+02:00",
    "quoteTimestamp": 1761162295020,
    "ticker": "NVDA",
    "price": 184.57
}
```

<br>
<br>

<h5>Quotation history</h5>
METHOD: `GET`    
URI: `/api/v1/quotation/NVDA/100/DESC`
> Use this endpoint to fetch historical quotations for any supported ticker. 100 is amount of quotations (to move to proper pagination)

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
> Use this endpoint to make order BUY or SELL type

Buy order request body example:
```
{
  "orderDirection": "BUY",
  "orderType": "MARKET",
  "ticker": "NVDA",
  "volume": 7
}
```
Buy order response body:
```
{
    "orderDirection": "BUY",
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
  "orderDirection": "SELL",
  "orderType": "MARKET",
  "ticker": "NVDA",
  "price": "185.10",
  "volume": 10
}
```

Sell order response body:
```
{
    "orderDirection": "SELL",
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