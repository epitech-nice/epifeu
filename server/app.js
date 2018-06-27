var express = require('express'),
    app = express(),
    axios = require('axios'),
    bodyParser = require('body-parser'),
    child_process = require('child_process'),
    cookieParser = require('cookie-parser'),
    session = require('express-session');

app.use(cookieParser());
app.use(session({
    secret: '2C44-4D44-WppQ38S',
    resave: true,
    saveUninitialized: true,
    cookie: {
      maxAge: 30 * 60 * 1000,
      httpOnly: true
    }
}));

var state = "RED";
child_process.exec('gpio mode 0 out');
child_process.exec('gpio write 0 1');

app.use(bodyParser.urlencoded({extended: false}));
app.use(bodyParser.json());
app.all('/*', function(req, res, next) {
  res.header("Access-Control-Allow-Origin", "*");
  res.header("Access-Control-Allow-Methods", "GET,PUT,POST,DELETE,PATCH");
  res.header("Access-Control-Allow-Headers", "X-Requested-With, Content-Type, Authorization");
  next();
})
// Authentication and Authorization Middleware
var auth = function(req, res, next) {
  console.log(req.session);
  if (req.session && req.session.admin === true)
    return next();
  else
    return res.sendStatus(401);
};

// Login endpoint
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

// Logout endpoint
app.get('/logout', function (req, res) {
  req.session.destroy();
  res.send({msg: "Success"});
});

// Get content endpoint
app.post('/state', auth, function (req, res) {
  if (state == "RED") {
    child_process.exec('gpio write 0 0');
    state = "GREEN";
  } else {
    child_process.exec('gpio write 0 1');
    state = "RED";
  }
  res.send({msg: "Changed", data: {
    state: state
  }});
});

app.listen(3000, '0.0.0.0');
console.log("app running at http://localhost:3000");
