'use strict';

var compression =   require('compression'),
    mysql =         require('mysql'),
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
        host                : 'localhost',
        user                : 'aws',
        password            : 'aws',
        database            : 'aws_data',
    //    port                : 5329,
        insecureAuth        : true
      });

app.use(compression(
{
    filter : function ( req, res) { return true; },
    level : 1,
    memLevel : 9

}));
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

function handleQuery(sql)
{
    return function(req, res)
    {
	var q = sql.slice(0); //ewww, make a copy
        if(req.params.amount)
            q = q.replace(":amount:", req.params.amount);
        if(req.params.begindate)
            q = q.replace(":begindate:", pool.escape(req.params.begindate));
        if(req.params.enddate)
            q = q.replace(":enddate:", pool.escape(req.params.enddate));
	console.log(q);
        var query = pool.query(q);//"SELECT * FROM measurement LIMIT 10");
	var first = true;

	query.on('error', function(err)
	{
	    console.log("Error in query: " + err);
	})
	.on('result', function(row)
	{
            if(req.params.type == 'cbor')
                res.send(cbor.encode(row));
	    else if(req.params.type == 'bin')
	    {
		var buffer = Buffer.alloc(16 * 2 + 0); //DATE?
		buffer.writeInt16BE(row.barometer, 0);
		buffer.writeInt16BE(row.insideTemp, 2);
		buffer.writeInt16BE(row.insideHum, 4);
		buffer.writeInt16BE(row.outsideTemp, 6);
		buffer.writeInt16BE(row.outsideHum, 8);
		buffer.writeInt16BE(row.windSpeed, 10);
		buffer.writeInt16BE(row.avgWindSpeed, 12);
		buffer.writeInt16BE(row.windDir, 14);
		buffer.writeInt16BE(row.rainRate, 16);
		buffer.writeInt16BE(row.UVLevel, 18);
		buffer.writeInt16BE(row.solarRad, 20);
		buffer.writeInt16BE(row.xmitBatt, 22);
		buffer.writeInt16BE(row.battLevel, 24);
		buffer.writeInt16BE(row.sunRise, 26);
		buffer.writeInt16BE(row.sunset, 28);
		buffer.writeInt16BE(row.stationId, 30);
		res.write(buffer);
	    }
            else
	    {
	        if(first)
	    	    res.write('[');
		else
		    res.write(',');
                res.write(JSON.stringify(row));
	    }
	    first = false;
        })
	.on('end', function()
	{
            if(req.params.type == 'cbor')
                res.end();
	    else if(req.params.type == 'bin')
	    {
		res.end();
	    }
            else
	    {
		if(first)
		    res.write('[');
		res.write(']');
		res.end();
	    }
	});
    }
}
