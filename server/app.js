var express = require('express'),
    app = express(),
    axios = require('axios'),
    bodyParser = require('body-parser'),
    child_process = require('child_process'),
    cookieParser = require('cookie-parser'),
    session = require('express-session'),
    localIP = require('quick-local-ip'),
    os = require('os'),
    fs = require('fs'),
    MemoryStore = session.MemoryStore,
    https = require('https');

app.use(cookieParser());
app.use(session({
    secret: '2C44-4D44-WppQ38S',
    resave: true,
    saveUninitialized: true,
    store: new MemoryStore(),
    maxAge: Date.now() + (24 * 60 * 60 * 1000),
    cookie: {
      maxAge: Date.now() + (24 * 60 * 60 * 1000),
      httpOnly: true,
      secure: false
    }
}));

//var ipAddress = getLocalIP();
var ipAddress = localIP.getLocalIP4();
var state = "RED";
child_process.exec('gpio mode 0 out');
child_process.exec('gpio write 0 1');

app.use(bodyParser.urlencoded({extended: false}));
app.use(bodyParser.json());

function getLocalIP() {
var interfaces = os.networkInterfaces();
  for (var k in interfaces) {
    for (var k2 in interfaces[k]) {
      var address = interfaces[k][k2];
      if (address.family === 'IPv4' && !address.internal && address.address != "127.0.0.1") {
        return address.address;
      }
    }
  }
}

var auth = function(req, res, next) {
  if (req.session)
    return next();
  else
    return res.status(401).send({msg: "Unauthorized"});
};

var admin = function(req, res, next) {
  if (req.session && req.session.admin === true)
    return next();
  else
    return res.status(401).send({msg: "Unauthorized"});
};

app.post('/login', function (req, res) {
  if (!req.body.cookie) {
    res.status(401).send('login failed');
  } else {
    axios.request({
      url: "https://intra.epitech.eu/user/?format=json",
      method: "get",
      headers: {
        Cookie: req.body.cookie
      }
    })
    .then(function(response) {
      req.session.picture = response.data.picture;
      req.session.name = response.data.title;
      req.session.admin = false;
      var groups = response.data.groups;
      for (var i = 0, len = groups.length; i < len; i++) {
        if (groups[i].title == 'DPR-NICE') {
          req.session.admin = true;
        }
      }
      req.session.save(function(err) {
        if (err) {
          console.log(err);
        }
      })
      res.status(200).send({
        msg: "Success",
        data: {
          picture: req.session.picture,
          name: req.session.name,
          admin: req.session.admin,
          state: state
        }
      });
    })
    .catch(function(error) {
      res.status(401).send({msg: "Error"})
    });
  }
});

app.get('/logout', auth, function (req, res) {
  req.session.destroy();
  res.send({msg: "Success"});
});

app.post('/state', admin, function (req, res) {
  if (state == "RED") {
    child_process.exec('gpio write 0 0');
    state = "GREEN";
  } else {
    child_process.exec('gpio write 0 1');
    state = "RED";
  }
  console.log("Status changé en [" + state + "] par [" + req.session.name + "]");
  res.send({msg: "Changed", data: {
    admin: req.session.admin,
    state: state
  }});
});

app.get('/state', auth, function (req, res) {
  res.send({msg: "State", data: {
    state: state,
    admin: req.session.admin
  }});
});

app.get('/', function(req, res) {
  /*if (ipAddress == "127.0.0.1") {
    ipAddress = getLocalIP();
  }*/

  res.status(200).send({msg: "Up", data: {ip: ipAddress}});
});


var sslOptions = {
  key: fs.readFileSync('/home/pi/ssl/epifeu.key'),
  cert: fs.readFileSync('/home/pi/ssl/epifeu.crt'),
  passphrase: "epitech42"
};
//  key: fs.readFileSync('./ssl/epifeu.key'),

https.createServer(sslOptions, app).listen(3000, '0.0.0.0');

//app.listen(3000, '0.0.0.0');
console.log("app running at http://localhost:3000");
