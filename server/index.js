'use strict';

var mysql =         require('mysql'),
    express =       require('express'),
    bodyParser =    require('body-parser'),
    morgan =        require('morgan'),
    http =          require('http'),
    app =           express(),
    fs =            require('fs'),
    path =          require('path'),
    cbor =          require('cbor'),
    pool =          mysql.createPool({
        connectionLimit     : 10,
        host                : '145.48.203.28',
        user                : 'aws',
        password            : 'aws',
        database            : 'aws_data',
        port                : 5329,
        insecureAuth        : true
      });

app.use(bodyParser.urlencoded({extended: false}));
app.use(bodyParser.json());
app.use(morgan('tiny'));

var server = http.createServer(app);
app.use(express.static('static'));
app.get("/", function(req,res)
{
    pool.query("select * from `measurement` ORDER BY `timestamp` LIMIT 1", function(error, results, fields)
    {
    //    res.send(cbor.encode(results[0]));
        res.send(results[0]);
//        res.sendFile(path.join(__dirname, "static/index.htm"));
    });
});

app.get("/lasthours/:amount/:type?", handleQuery("select * from `measurement` WHERE `timestamp` > NOW() - INTERVAL :amount: HOUR"));
app.get("/lastdays/:amount/:type?", handleQuery("select * from `measurement` WHERE `timestamp` > NOW() - INTERVAL :amount: DAY"));
app.get("/lastmonths/:amount/:type?", handleQuery("select * from `measurement` WHERE `timestamp` > NOW() - INTERVAL :amount: MONTH"));
app.get("/between/:begindate/:enddate/:type?", handleQuery("select * from `measurement` WHERE `timestamp` BETWEEN :begindate: AND :enddate:"));


server.listen(1337);
console.log("Listening for http on port 1337");

function handleQuery(query)
{
    return function(req, res)
    {
        if(req.params.amount)
            query = query.replace(":amount:", req.params.amount);
        if(req.params.begindate)
            query = query.replace(":begindate:", pool.escape(req.params.begindate));
        if(req.params.enddate)
            query = query.replace(":enddate:", pool.escape(req.params.enddate));
        pool.query(query, function(error, results, fields)
        {
            if(error)
                console.log(error);
            if(req.params.type == 'cbor')
                res.send(cbor.encode(results));
            else
                res.send(results);
        });
    }
};
