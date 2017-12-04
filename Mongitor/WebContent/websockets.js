/**
 * 
 */

var ws;
var backlog = [];
var mainright_empty = "";
const trafficlights = ["traffic_white.png", "traffic_green.png", "traffic_orange.png", "traffic_red.png"];
var replicationlags = [];

function sendBacklog()
{
  while ((backlog.length > 0) && (ws.readyState == ws.OPEN))
  {
    ws.send(backlog[0]);
    backlog.shift();
  }
}

function setupWebSocket()
{
  ws = new WebSocket("ws://" + window.location.hostname + ":"+websocketport+"/");
  ws.onopen = function()
  {
    if ($("#mainstatus").html() == "Waiting for connection to Mongitor")
    {
      showstatus("Connected to Mongitor", true);
    }  
    sendBacklog();
  };

  ws.onmessage = function(evt)
  {
    console.log(evt.data);
    var message_obj = JSON.parse(evt.data);
    if (message_obj.type == "response")
    {
      handleResponse(message_obj);
    }
  };

  ws.onerror = function(err)
  {
    // probably caused by timeout: no action
  };

  ws.onclose = function()
  {
    // probably caused by timeout: reconnect in a second
    setTimeout(setupWebSocket, 1000);
  };
}

function sendOverWebSocket(id, type, msg)
{
  msg.id = id;
  msg.type = type;

  backlog.push(JSON.stringify(msg));
  sendBacklog();
}

function find_lag_per_server(server)
{
	var i = 0;
	while (i < replicationlags.length)
	{
		if (replicationlags[i]['server'] == server)
		{
			return replicationlags[i];
		}
		i++;
	}	
	return null;
}

function show_database_details(database)
{
  show_div($("#mainright_databasedetails")); 
  $("#mainright_databasedetails").html("Waiting for results").css('color', '#494747').css('margin-left', '20px').css('margin-top', '30px');
  sendOverWebSocket("databasedetails", "command", {database: database});
}

function show_collection_details(database, collection)
{
  show_div($("#mainright_collection")); 
  $("#div_collection_stats").html("Waiting for results");
  $("#collectionname1").html(collection);
  $("#collectionname2").html(collection);
  $("#collectionname3").html(collection);
  sendOverWebSocket("collectiondetails", "command", {database: database, collection: collection});
}

function convertToHtml(obj, prefix)
{
  var html = "";

  //is object
  //    Both arrays and objects seem to return "object"
  //    when typeof(obj) is applied to them. So instead
  //    I am checking to see if they have the property
  //    join, which normal objects don't have but
  //    arrays do.
  if (obj == undefined)
  {
    return String(obj) + "\n";
  }
  
  if (typeof(obj) == "object" && (obj.join == undefined))
  {
    // is object
    prefix = prefix + "  ";
    html = html + "\n";
    for (prop in obj)
    {
      if (obj.hasOwnProperty(prop))
      {  
        html = html + prefix + prop + ": " + convertToHtml(obj[prop], prefix);
      }
    }  
    return html;
  }
    
  if (typeof(obj) == "object" && !(obj.join == undefined))
  {
    //is array
    prefix = prefix + "  ";
    html = html + "\n";
    for (prop in obj)
    {
      html = html + prefix + convertToHtml(obj[prop], prefix);
    }
    return html;
  }  

  return String(obj) + "\n";
}

function add_replicationlag(server, lag)
{
	var server_obj = find_lag_per_server(server);
	if (server_obj == null)
	{
		var lag_array = [{date: Date.now(), replicationlag: lag}];
		server_obj = {server: server, lags: lag_array};
		replicationlags.push(server_obj);
	}	
	else
	{
		server_obj['lags'].push({date: Date.now(), replicationlag: lag});
		if (server_obj['lags'].length > 200)
		{
			server_obj['lags'].shift();
		}	
	}
	if (($("#mainright_serverdetails").is(":visible")) && (id == server))
	{
	  updateChart();
	}    
}

function update_configservers(message_obj)
{
	var configservers_array = message_obj['configservers'];
	configservers_array.forEach(function(configserver_obj)
	{
	  if ("mongitorstate" in configserver_obj && configserver_obj['mongitorstate'] > 0)
	  {
		var statecolor = "red";
		if ((configserver_obj['state'] == "PRIMARY") || (configserver_obj['state'] == "SECONDARY"))
		{
		  statecolor = "green";
		}	
		if (configserver_obj['state'] == "?")
		{
		  statecolor = "#494747";
		}
		$("#" + configserver_obj['hostname'] + "_state").css("color", statecolor);
		$("#" + configserver_obj['hostname'] + "_state").html(configserver_obj['state']);
		$("#" + configserver_obj['hostname'] + "_traffic").attr("src","mongitor_images/" + trafficlights[configserver_obj['mongitorstate']]);
		
		var connectedcolor = configserver_obj['connected'] ? "green" : "red";
	    var connectedtext = configserver_obj['connected'] ? "Yes" : "No";
	    $("#" + configserver_obj['hostname'] + "_connected").html(connectedtext).css("color", connectedcolor);
	    var mastertext = configserver_obj['master'] ? "Yes" : "No";
	    $("#" + configserver_obj['hostname'] + "_master").html(mastertext);
	  }
	}); 
}

function update_shards(message_obj)
{
	var shards_array = message_obj['shards'];
	shards_array.forEach(function(shard_obj)
	{  
	  if ('shardServers' in shard_obj)	
	  {        
	    var shardservers_array = shard_obj['shardServers'];
	    shardservers_array.forEach(function(shardserver_obj)
	    {
	    	if ("mongitorstate" in shardserver_obj && shardserver_obj['mongitorstate'] > 0)
	      {
	    	var statecolor = "red";
	    	if ((shardserver_obj['state'] == "PRIMARY") || (shardserver_obj['state'] == "SECONDARY"))
	    	{
	    	  statecolor = "green";
	    	}	
	    	if (shardserver_obj['state'] == "?")
	    	{
	    	  statecolor = "#494747";
	    	}
	    	$("#" + shardserver_obj['hostname'] + "_state").css("color", statecolor);
	    	$("#" + shardserver_obj['hostname'] + "_state").html(shardserver_obj['state']);
	    	$("#" + shardserver_obj['hostname'] + "_traffic").attr("src","mongitor_images/" + trafficlights[shardserver_obj['mongitorstate']]);
	    	
	    	var connectedcolor = shardserver_obj['connected'] ? "green" : "red";
	        var connectedtext = shardserver_obj['connected'] ? "Yes" : "No";
	        $("#" + shardserver_obj['hostname'] + "_connected").html(connectedtext).css("color", connectedcolor);
	        var mastertext = shardserver_obj['master'] ? "Yes" : "No";
	        $("#" + shardserver_obj['hostname'] + "_master").html(mastertext);
	      }
	    }); 
	  }
	});  
}

function addhtml(div, html)
{
  $("#"+ div).html($("#"+ div).html() + html);
}

function display_size(size)
{
  function my_round(my_size)
  {
    if (my_size < 100)
    {
      return Math.round(my_size * 10) / 10;
    }  
    else
    {
      return Math.round(my_size);
    }  
  }
  var tmp;
  if (size < 1024)
  {
    return size;
  }  
  tmp = size / 1024;
  if (tmp < 1024)
  {
    return my_round(tmp) + " KB"
  } 
  tmp = tmp / 1024;
  if (tmp < 1024)
  {
    return my_round(tmp) + " MB"
  } 
  tmp = tmp / 1024;
  if (tmp < 1024)
  {
    return my_round(tmp) + " GB"
  } 
  tmp = tmp / 1024;
  if (tmp < 1024)
  {
    return my_round(tmp) + " TB"
  } 
}

function handleResponse(response)
{
  if (halted)
  {
    return;
  }  
  if (response.id == "init")
  {  
    if (response.message == "nocredentialsfile")
    {
      $("#setPasswordModal").show();
      $("#initialpassword").focus();
    } 
    if (response.message == "getpassword")
    {
      $("#enterPasswordModal").show();
      $("#mongitorpassword").focus();
    }
  }
  else if (response.id == "setpassword")
  {  
    if (response.ok)
    {
      showstatus("Mongitor password was set", true);
      sendOverWebSocket("defaultcredentialsexist", "command", {});
    }  
    else
    {
      showstatus("Failed to set password: " + response.error, false);
    }  
  }  
  else if (response.id == "checkpassword")
  {  
    if (response.ok)
    {
      showstatus("Logged in to Mongitor", true);
      $("#enterPasswordModal").hide();
      sendOverWebSocket("defaultcredentialsexist", "command", {});
    }  
    else
    {
      $("#enterpasswordstatus").html(response.error);
      $("#enterpasswordstatus").css('color', 'red');
    }  
  }  
  else if (response.id == "defaultcredentialsexist")
  {  
    if (response.ok)
    {
      if (response.message == "yes")
      {  
        showstatus("Mongo Router credentials found", true);
        sendOverWebSocket("listcredentials", "command", {});
        sendOverWebSocket("detectcluster", "command", {});
      }
      else
      {
        $("#enterDefaultCredentials").show();
        $("#inputrouterhost").focus();
      }  
    }  
    else
    {
      showstatus("Error checking for default credentials: " + response.error, false);
    }  
  }
  else if (response.id == "connecttorouter")
  {  
    if (response.ok)
    {
      showstatus("Credentials for Mongo Router are OK", true);
      $("#enterDefaultCredentials").hide();
      sendOverWebSocket("listcredentials", "command", {});
      sendOverWebSocket("detectcluster", "command", {});
    }  
    else
    {
      $("#enterdefaultcredentialsstatus").html(response.error);
      $("#enterdefaultcredentialsstatus").css('color', 'red');
    }  
  }
  else if (response.id == "detectcluster")
  {  
    if (response.ok)
    {
      if (mainright_empty == "")
      {  
    	// save empty layout
        mainright_empty = $("#mainright").html();
      }
      else
      {
    	// reset to empty layout, before appending results
        $("#mainright").html(mainright_empty);
      }  
      showstatus("Cluster discovered", true);
      // updateclusterstatus(response.message);
      // var message_obj = JSON.parse({ router:
		// {hostname:"irak",port:27017,connected:false,_id:"irak:27017"},
		// configservers:
		// [{hostname:"drenthe",port:27017,connected:false,master:false,state:"",_id:"drenthe:27017"},{hostname:"flevoland",port:27017,connected:false,master:false,state:"",_id:"flevoland:27017"},{hostname:"overijssel",port:27017,connected:false,master:false,state:"",_id:"overijssel:27017"}],
		// shards: []});
      // , shards:
		// [{\"id\":\"noord\",\"shardServers\":[{\"hostname\":\"friesland\",\"port\":27017,\"connected\":false,\"master\":false,\"state\":\"?\",\"_id\":\"friesland:27017\"},{\"hostname\":\"groningen\",\"port\":27017,\"connected\":false,\"master\":false,\"state\":\"?\",\"_id\":\"groningen:27017\"},{\"hostname\":\"noordholland\",\"port\":27017,\"connected\":false,\"master\":false,\"state\":\"?\",\"_id\":\"noordholland:27017\"}]},{\"id\":\"midden\",\"shardServers\":[{\"hostname\":\"gelderland\",\"port\":27017,\"connected\":false,\"master\":false,\"state\":\"?\",\"_id\":\"gelderland:27017\"},{\"hostname\":\"utrecht\",\"port\":27017,\"connected\":false,\"master\":false,\"state\":\"?\",\"_id\":\"utrecht:27017\"},{\"hostname\":\"zuidholland\",\"port\":27017,\"connected\":false,\"master\":false,\"state\":\"?\",\"_id\":\"zuidholland:27017\"}]},{\"id\":\"zuid\",\"shardServers\":[{\"hostname\":\"limburg\",\"port\":27017,\"connected\":false,\"master\":false,\"state\":\"?\",\"_id\":\"limburg:27017\"},{\"hostname\":\"noordbrabant\",\"port\":27017,\"connected\":false,\"master\":false,\"state\":\"?\",\"_id\":\"noordbrabant:27017\"},{\"hostname\":\"zeeland\",\"port\":27017,\"connected\":false,\"master\":false,\"state\":\"?\",\"_id\":\"zeeland:27017\"}]}]
		// });
      // var message_obj = JSON.parse("{ \"router\":
		// {\"hostname\":\"irak\",\"port\":27017,\"connected\":false,\"_id\":\"irak:27017\"}
		// , \"configservers\":
		// [{\"hostname\":\"drenthe\",\"port\":27017,\"connected\":false,\"master\":false,\"state\":\"\",\"_id\":\"drenthe:27017\"},{\"hostname\":\"flevoland\",\"port\":27017,\"connected\":false,\"master\":false,\"state\":\"\",\"_id\":\"flevoland:27017\"},{\"hostname\":\"overijssel\",\"port\":27017,\"connected\":false,\"master\":false,\"state\":\"\",\"_id\":\"overijssel:27017\"}],
		// \"shards\":
		// [{\"id\":\"noord\",\"shardServers\":[{\"hostname\":\"friesland\",\"port\":27017,\"connected\":false,\"master\":false,\"state\":\"?\",\"_id\":\"friesland:27017\"},{\"hostname\":\"groningen\",\"port\":27017,\"connected\":false,\"master\":false,\"state\":\"?\",\"_id\":\"groningen:27017\"},{\"hostname\":\"noordholland\",\"port\":27017,\"connected\":false,\"master\":false,\"state\":\"?\",\"_id\":\"noordholland:27017\"}]},{\"id\":\"midden\",\"shardServers\":[{\"hostname\":\"gelderland\",\"port\":27017,\"connected\":false,\"master\":false,\"state\":\"?\",\"_id\":\"gelderland:27017\"},{\"hostname\":\"utrecht\",\"port\":27017,\"connected\":false,\"master\":false,\"state\":\"?\",\"_id\":\"utrecht:27017\"},{\"hostname\":\"zuidholland\",\"port\":27017,\"connected\":false,\"master\":false,\"state\":\"?\",\"_id\":\"zuidholland:27017\"}]},{\"id\":\"zuid\",\"shardServers\":[{\"hostname\":\"limburg\",\"port\":27017,\"connected\":false,\"master\":false,\"state\":\"?\",\"_id\":\"limburg:27017\"},{\"hostname\":\"noordbrabant\",\"port\":27017,\"connected\":false,\"master\":false,\"state\":\"?\",\"_id\":\"noordbrabant:27017\"},{\"hostname\":\"zeeland\",\"port\":27017,\"connected\":false,\"master\":false,\"state\":\"?\",\"_id\":\"zeeland:27017\"}]}]
		// }");
      var message_obj = JSON.parse(response.message);
      if ('router' in message_obj)
      {  
        var router_obj = message_obj['router'];
        var connectedcolor = router_obj['connected'] ? "green" : "red";
        var connectedtext = router_obj['connected'] ? "Yes" : "No";
        $("#div_router").html
        (`
          <div class='server_left'>
            <img src='mongitor_images/traffic_green.png'>
            <img src='mongitor_images/server.jpg'>
          </div>  
          <div class='server_right'>
            <span class='servername'>` + router_obj['_id'] + `</span><br><br>
            Connected: <span id='router_connected' style='color: ` + connectedcolor + `'>` + connectedtext + `</span><br>
          </div>
        `);
        $("#title2").html(" - " + router_obj['_id']);
      }
      if ('configservers' in message_obj)
      {
        var configservers_array = message_obj['configservers'];
        configservers_array.forEach(function(configserver_obj)
        {
          if ("_id" in configserver_obj)
          {
            $("#div_configservers").html($("#div_configservers").html() +
            `
              <div class='server_left'>
                <img id='` + configserver_obj['hostname'] + `_traffic' src='mongitor_images/traffic_white.png'>
                <img src='mongitor_images/server.jpg' class='clickable' onclick='serverdetails("` + configserver_obj['_id'] + `","` + configserver_obj['hostname'] + `")'>
              </div>  
              <div class='server_right'>
                <span class='servername'>` + configserver_obj['_id'] + `</span><br><br>
                Connected: <span id='` + configserver_obj['hostname'] + `_connected' style='color: #494747'>?</span><br>
                Master: <span id='` + configserver_obj['hostname'] + `_master'>?</span><br>
                State: <span id='` + configserver_obj['hostname'] + `_state' style='color: #494747'>?</span>
              </div>
            `);
          }  
        });
      }  
      if ('shards' in message_obj)
      {
        var shards_array = message_obj['shards'];
        shards_array.forEach(function(shard_obj)
        {
          if ("id" in shard_obj)
          {
            $("#mainright").html($("#mainright").html() +
            `
              <div class='hrlabel'>Shard ` + shard_obj['id'] + `</div>
              <hr>
              <div class='showhide' id='showhide_shard' onclick='showhide($(this), $("#div_shard_` + shard_obj['id'] + `"))'>-</div>
              <div id='div_shard_` + shard_obj['id'] + `' class='div_shard'>
              </div>
            `);
            if ("shardServers" in shard_obj)
            {
              var shardservers_array = shard_obj['shardServers'];
              shardservers_array.forEach(function(shardserver_obj)
              {
                if ("_id" in shardserver_obj)
                {
                  $("#div_shard_" + shard_obj['id']).html($("#div_shard_" + shard_obj['id']).html() +
                  `
                   <div class='server_left'>
                     <img id='` + shardserver_obj['hostname'] + `_traffic' src='mongitor_images/traffic_white.png'>
                     <img src='mongitor_images/server.jpg' class='clickable' onclick='serverdetails("` + shardserver_obj['_id'] + `","` + shardserver_obj['hostname'] + `")'>
                   </div>  
                   <div class='server_right'>
                     <span class='servername'>` + shardserver_obj['_id'] + `</span><br><br>
                     Connected: <span id='` + shardserver_obj['hostname'] + `_connected' style='color: #494747'>?</span><br>
                     Master: <span id='` + shardserver_obj['hostname'] + `_master'>?</span><br>
                     State: <span id='` + shardserver_obj['hostname'] + `_state' style='color: #494747'>?</span>
                   </div>
                 `);
                }  
              });
            }  
          }
        });
      }  
      // startstop_monitoring(true);
    }  
    else
    {
      showstatus("Failed to discover cluster: " + response.error, false);
    }  
  }
  else if (response.id == "checkcluster")
  {  
	check_is_running = false;  
    if (response.ok)
    {
      showstatus("Checked cluster at " + moment().format("DD-MM-YYYY HH:mm:ss") + ". No problems were found.", true);
    }  
    else
    {
      showstatus("Checked cluster at " + moment().format("DD-MM-YYYY HH:mm:ss") + ". Result: " + response.error, false);
    }
	  var message_obj = JSON.parse(response.message);
	  if ('router' in message_obj)
	  {  
	    var router_obj = message_obj['router'];
	    var connectedcolor = router_obj['connected'] ? "green" : "red";
	    var connectedtext = router_obj['connected'] ? "Yes" : "No";
	    $("#router_connected").html(connectedtext).css("color", connectedcolor);
	  }
	  if ('configservers' in message_obj)
	  {
		  update_configservers(message_obj); 
	  }
	  if ('shards' in message_obj)
	  {
		  update_shards(message_obj);  
	  }
  }
  else if (response.id == "updatecluster")
  {  
	  var message_obj = JSON.parse(response.message);
	  if ('configservers' in message_obj)
	  {
		  update_configservers(message_obj); 
	  }
	  if ('shards' in message_obj)
	  {
		  update_shards(message_obj);  
	  }
  }
  else if (response.id == "getclusterinfo")
  { 
    if (response.ok)
    {
      var message_obj = JSON.parse(response.message);
      
      if ('enabled' in message_obj)
      {
        $("#balancer_checkbox").prop('checked', message_obj.enabled); 
      }
      if ('running' in message_obj)
      {
        if (message_obj.running)
        {
          $("#balancer_text").html("Balancer is currently running").css('color', '#494747');
        }  
        else
        {
          $("#balancer_text").html("Balancer is currently not running").css('color', '#494747');
        } 
      }  
    }  
    else
    {
      $("#balancer_text").html(response.error).css('color', 'red');
    }
  }
  else if (response.id == "setbalancer")
  { 
    if (response.ok)
    {
      sendOverWebSocket("getclusterinfo", "command", {});
    }  
    else
    {
      $("#balancer_text").html(response.error).css('color', 'red');
    }
  }
  else if (response.id == "listcredentials")
  {
    var creds_obj = JSON.parse(response.message);
    var html = "<table id='credentialstable'><tr><th>Label</th><th>Username</th><th>Password</th></tr>";
    var counter = 1;
    for (var key in creds_obj)
    {
      var value = creds_obj[key];
      if (key == "default")
      {
        if (!("username" in value) || !("password" in value))
        {
          $("#routercredentialsstatus").html("No default username or password found");
        } 
        else
        {  
          var amp = value.username.lastIndexOf("@");
          if (amp < 1)
          {
            $("#routercredentialsstatus").html("Could not determine username part of string");
          } 
          else
          {
            var hostport = value.username.substr(amp+1);
            var username = value.username.substr(0, amp);
            var colon = hostport.indexOf(":");
            if (colon < 1)
            {
              $("#routercredentialsstatus").html("No host or port found");
            } 
            else
            {
              var host = hostport.substr(0, colon);
              var port = hostport.substr(colon+1);
              $("#updaterouterhost").val(host);
              $("#updaterouterport").val(port);
              $("#updaterouterusername").val(username);
              $("#updaterouterpassword").val(value.password);
            }  
          }  
        }  
      }  
      else
      {
        html = html + "<tr><td><input id='" + counter + "' placeholder='label' value='" + key + "'></td><td>";
        html = html + "<input id='user" + counter + "' placeholder='username' value='" + value.username + "'></td><td><input id='password" + counter + "' placeholder='password' value='" + value.password + "'></td></tr>";
        counter++;
      }  
    }
    html = html + "<tr><td><input  id='" + counter + "'placeholder='label'></td><td><input id='user" + counter + "' placeholder='username'></td><td><input id='password" + counter + "' placeholder='password'></td></tr></table>";
    $("#other_credentials").html(html);
  }
  else if (response.id == "updateothercredentials")
  {  
    if (response.ok)
    {
      $("#othercredentialsstatus").html("");
      sendOverWebSocket("listcredentials", "command", {});
    }  
    else
    {
      $("#othercredentialsstatus").html(response.error);
    }  
  }
  else if (response.id == "updatedefaultcredentials")
  {  
    if (response.ok)
    {
      $("#routercredentialsstatus").html("Connection OK").css('color', '#494747');
    }  
    else
    {
      $("#routercredentialsstatus").html(response.error).css('color', 'red');
    }  
  }  
  else if (response.id == "replicationlag")
  { 
	  var message_obj = JSON.parse(response.message);
	  if (('server' in message_obj) && ('lag' in message_obj))
	  {
		  add_replicationlag(message_obj['server'], message_obj['lag']); 
	  }
  }
  else if (response.id == "mongocommand")
  {  
    if (response.ok)
    {
      var message_obj = JSON.parse(response.message);
      var html = convertToHtml(message_obj, "");
      $("#server_command_output").html(html).css('color', '#494747');
    }  
    else
    {
      $("#server_command_output").html(response.error).css('color', 'red');
    }  
  }
  else if (response.id == "listdatabases")
  {  
    if (response.ok)
    {
      var html = "";
      var message_obj = JSON.parse(response.message);
      if ("databases" in message_obj)
      {
        databases_array = message_obj['databases'];
        // sort on name
        databases_array.sort(function(a,b) {return (a.name > b.name) ? 1 : ((b.name > a.name) ? -1 : 0);} );
        html = "<table id='databases_table'>";
        databases_array.forEach(function(database_obj)
        {
          html = html + "<tr>";
          html = html + "<td class='database_link' onclick='show_database_details(\"" + database_obj['name'] + "\")'>" + database_obj['name'] + "</td>";
          if ("sizeOnDisk" in database_obj)
          {
            var size;
            if (typeof database_obj['sizeOnDisk'] === 'object')
            {
              size = Number(database_obj['sizeOnDisk']['$numberLong']);
            }  
            else
            {
              size = database_obj['sizeOnDisk'];
            }  
            html = html + "<td>" + display_size(size) + "</td>";
          } 
          else
          {
            html = html + "<td>&nbsp;</td>";
          }  
          if ("shards" in database_obj)
          {
            var size;
            if (typeof database_obj['sizeOnDisk'] === 'object')
            {
              html = html + "<td>";
              shards_obj = database_obj['shards'];
              var first = true;
              
                for (var propertyName in shards_obj)
                {
                  if (first)
                  {
                    first = false;
                  }  
                  else
                  {
                    html = html + ", ";
                  }  
                  html = html + propertyName + ": ";
                  var size;
                  if (typeof shards_obj[propertyName] === 'object')
                  {
                    size = Number(shards_obj[propertyName]['$numberLong']);
                  }  
                  else
                  {
                    size = shards_obj[propertyName];
                  }  
                  html = html + display_size(size);
                }
              
              html = html + "</td>";
            }  
            else
            {
              html = html + "<td>&nbsp;</td>";
            }  
          } 
          else
          {
            html = html + "<td>&nbsp;</td>";
          }          
          html = html + "</tr>";
        });
        html = html + "</table>";
      }  
      
      $("#div_databases").html(html).css('color', '#494747');
    }  
    else
    {
      $("#div_databases").html(response.error).css('color', 'red');
    }  
  }
  else if (response.id == "databasedetails")
  {  
    if (response.ok)
    {
      var html = "";
      var message_obj = JSON.parse(response.message);
      var database = "?";
      if ("database" in message_obj)
      {
        database = message_obj['database'];
      } 
      
      html = html +
      `
        <div class='hrlabel'>Collections in database ` + database + `</div>
        <hr>
        <div class='showhide' id='showhide_collections' onclick='showhide($(this), $("#div_collections"))'>-</div>
        <div id='div_collections'>
        
      `;
      if ("collections" in message_obj)
      {
        collections_array = message_obj['collections'];
        collections_array.sort();
        html = html + "<table id='collections_table'>";
        collections_array.forEach(function(collection)
        {
          html = html + "<tr>";
          html = html + "<td class='collection_link' onclick='show_collection_details(\"" + database + "\",\"" + collection + "\")'>" + collection + "</td>";
          html = html + "</tr>";
        });
        html = html + "</table></div>";
      }  
      $("#mainright_databasedetails").html(html).css('color', '#494747');
    }  
    else
    {
      $("#mainright_databasedetails").html(response.error).css('color', 'red');
    }  
  }
  else if (response.id == "collectiondetails")
  {  
    if (response.ok)
    {
      var html = "<table id='collection_stats_table class='collection_details_table''>";
      var message_obj = JSON.parse(response.message);
      var sharded = "?";
      if ("sharded" in message_obj)
      {
        sharded = message_obj['sharded'] ? "yes" : "no";
      } 
      html = html + "<tr><td>Sharded:</td><td>" + sharded + "</td></tr>";
      var capped = "?";
      if ("capped" in message_obj)
      {
        capped = message_obj['capped'] ? "yes" : "no";
      } 
      html = html + "<tr><td>Capped:</td><td>" + capped + "</td></tr>";
      var count = "?";
      if ("count" in message_obj)
      {
        count = message_obj['count'];
      } 
      html = html + "<tr><td>Count:</td><td>" + count + "</td></tr>";     
      var size = "?";
      if ("size" in message_obj)
      {
        if (typeof message_obj['size'] === 'object')
        {
          size = Number(message_obj['size']['$numberLong']);
        }  
        else
        {
          size = message_obj['size'];
        }  
        size = display_size(size);
      } 
      html = html + "<tr><td>Size:</td><td>" + size + "</td></tr>";
      size = "?";
      if ("storageSize" in message_obj)
      {
        if (typeof message_obj['storageSize'] === 'object')
        {
          size = Number(message_obj['storageSize']['$numberLong']);
        }  
        else
        {
          size = message_obj['storageSize'];
        }  
        size = display_size(size);
      } 
      html = html + "<tr><td>Storagesize:</td><td>" + size + "</td></tr>";
      size = "?";
      if ("totalIndexSize" in message_obj)
      {
        if (typeof message_obj['totalIndexSize'] === 'object')
        {
          size = Number(message_obj['totalIndexSize']['$numberLong']);
        }  
        else
        {
          size = message_obj['totalIndexSize'];
        }  
        size = display_size(size);
      } 
      html = html + "<tr><td>Indexsize:</td><td>" + size + "</td></tr>";
      var indexcount = "?";
      if ("nindexes" in message_obj)
      {
        indexcount = message_obj['nindexes'];
      } 
      html = html + "<tr><td>Number of indexes:</td><td>" + indexcount + "</td></tr>";
      var chunkcount = "?";
      if ("nchunks" in message_obj)
      {
        chunkcount = message_obj['nchunks'];
      } 
      html = html + "<tr><td>Number of chunks:</td><td>" + chunkcount + "</td></tr>";      
      html = html + "</table>";  
      $("#div_collection_stats").html(html).css('color', '#494747');
      
      if ("indexSizes" in message_obj)
      {
        html = "<table id='collection_indexes_table class='collection_details_table''>";
        var indexes_obj = message_obj['indexSizes'];
        for (var propertyName in indexes_obj)
        {
          html = html + "<tr><td>" + propertyName + ":</td><td>" + display_size(indexes_obj[propertyName]) + "</td></tr>";
        }
        html = html + "</table>";  
        $("#div_collection_indexes").html(html);
      }
      
      if ("shards" in message_obj)
      {
        html = "<table id='collection_shards_table' class='collection_details_table'><tr><th>shard</th><th>count</th><th>size</th><th>storagesize</th></tr><tr>";
        var shards_obj = message_obj['shards'];
        var data = [];
        for (var propertyName in shards_obj)
        {
          html = html + "<td>" + propertyName + "</td>";
          var shard_obj = shards_obj[propertyName];
          count = "?";
          if ("count" in shard_obj)
          {
            if (typeof shard_obj['count'] === 'object')
            {
              count = Number(shard_obj['count']['$numberLong']);
            }  
            else
            {
              count = shard_obj['count'];
            }  
          }
          html = html + "<td>" + count + "</td>";
          size = "?";
          if ("size" in shard_obj)
          {
            if (typeof shard_obj['size'] === 'object')
            {
              size = Number(shard_obj['size']['$numberLong']);
            }  
            else
            {
              size = shard_obj['size'];
            }  
            size = display_size(size);
          }
          html = html + "<td>" + size + "</td>";          
          size = "?";
          if ("storageSize" in shard_obj)
          {
            if (typeof shard_obj['storageSize'] === 'object')
            {
              size = Number(shard_obj['storageSize']['$numberLong']);
            }  
            else
            {
              size = shard_obj['storageSize'];
            }  
            data.push({"shard": propertyName, "size": size});
            size = display_size(size);
          }
          html = html + "<td>" + size + "</td>";          
          html = html + "</tr>";
        }
        html = html + "</table>";  
        $("#div_collection_shards").html(html);
        $("#div_shard_distribution_text").html("shard distribution");
        $("#div_shard_distribution").html("");
        show_pie(data);
      }
      else
      {
        $("#div_collection_shards").html("Collection is not sharded");
        $("#div_shard_distribution").html("");
        $("#div_shard_distribution_text").html("");
      }  
    }  
    else
    {
      $("#div_collection_stats").html(response.error).css('color', 'red');
    }  
  }
  else if (response.id == "getserverinfo")
  {  
    if (response.ok)
    {
      var message_obj = JSON.parse(response.message);
      if ('configservers' in message_obj)
      {
        $("#div_configservers_overview").html("");
        var configservers_array = message_obj['configservers'];
        configservers_array.forEach(function(configserver_obj)
        {
          if ("_id" in configserver_obj)
          {
            var os_obj = configserver_obj['os'];
            $("#div_configservers_overview").html($("#div_configservers_overview").html() +
            `
                <div class='server_info_div'>
                  <table class='server_info_table'>
                    <tr><th colspan=2>` + configserver_obj['_id'] + `</th></tr>
                    <tr>
                      <td>version:</td>
                      <td>` + configserver_obj['version'] + `</td>
                    </tr>
                    <tr>
                      <td>uptime:</td>
                      <td>` + configserver_obj['uptime'] + `</td>
                    </tr>
                    <tr>
                      <td>local time:</td>
                      <td>` + configserver_obj['local time'] + `</td>
                    </tr>
                    <tr>
                      <td>os:</td>
                      <td>` + os_obj['name'] + ` ` + os_obj['version'] + `</td>
                    </tr>
                  </table>    
                </div>
              `);
          }
          
        });
      }
      if ('shards' in message_obj)
      {
        var shards_array = message_obj['shards'];
        shards_array.forEach(function(shard_obj)
        {
          for (var shardname in shard_obj)
          {
            $("#mainright_servers").html($("#mainright_servers").html() +
            `
              <div class='hrlabel'>Shard ` + shardname + `</div>
              <hr>
              <div class='showhide' id='showhide_shard' onclick='showhide($(this), $("#div_shard_overview_` + shardname + `"))'>-</div>
              <div id='div_shard_overview_` + shardname + `' class='div_shard'>
              </div>
            `);
            
            shard_obj[shardname].forEach(function(shardserver_obj)
            {
              if ("_id" in shardserver_obj)
              {
                var os_obj = shardserver_obj['os'];
                var html =
                `
                    <div class='server_info_div'>
                      <table class='server_info_table'>
                        <tr><th colspan=2>` + shardserver_obj['_id'] + `</th></tr>
                        <tr>
                          <td>version:</td>
                          <td>` + shardserver_obj['version'] + `</td>
                        </tr>
                        <tr>
                          <td>uptime:</td>
                          <td>` + shardserver_obj['uptime'] + `</td>
                        </tr>
                        <tr>
                          <td>local time:</td>
                          <td>` + shardserver_obj['local time'] + `</td>
                        </tr>
                        <tr>
                          <td>os:</td>
                          <td>` + os_obj['name'] + ` ` + os_obj['version'] + `</td>
                        </tr>
                      </table>    
                    </div>
                  `;
                setTimeout(addhtml, 50, "div_shard_overview_" + shardname, html);
              }  
            });
          }
        });
      }  
    }  
    else
    {
      $("#mainright_servers").html(response.error).css('color', 'red');
    }  
  }
}
