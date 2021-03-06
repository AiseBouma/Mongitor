/**
 * 
 */
var id;
var server_obj;
var svg, xScale, yScale, xAxisCall, yAxisCall, valueline;
var chart_displayed;

var formatSecond = d3.timeFormat("");
var formatMinute = d3.timeFormat("%H:%M");
var formatHour = d3.timeFormat("%H:%M");

function multiFormat(date) 
{
  return (d3.timeMinute(date) < date ? formatSecond : d3.timeHour(date) < date ? formatMinute: formatHour)(date);
}

function renderChart()
  {
    data = server_obj['lags'];
    xScale.domain(d3.extent(data, function(d)
    {
      return d.date;
    }));
    yScale.domain([ 0, Math.max(1, d3.max(data, function(d)
    {
      return Math.round(d.replicationlag);
    })) ]);

    var l = svg.selectAll('.line').data([ data ]);
    var t = d3.transition().duration(250);

    // enter
    l.enter().append("path").attr("class", "line").transition(t).attr("d",
        valueline);

    // update
    l.transition(t).attr("d", valueline);

    // exit
    l.exit().remove();

    svg.select(".x").transition(t).call(xAxisCall);
    svg.select(".y").transition(t).call(yAxisCall);
  }

function buildChart()
{
  data = server_obj['lags'];
  var parentwidth = $("#div_graph").width() - 100;
  var margin = 
  {
    top : 20,
    right : 20,
    bottom : 30,
    left : 50
  };
  var width = parentwidth - margin.left - margin.right;
  var height = 300 - margin.top - margin.bottom;

  svg = d3.select("#div_graph").append("svg").attr("width",
      width + margin.left + margin.right).attr("height",
      height + margin.top + margin.bottom).attr('class', 'chart').append("g")
      .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

  xScale = d3.scaleTime().domain(d3.extent(data, function(d)
  {
    return d.date;
  })).range([ 0, width ]);
  yScale = d3.scaleLinear().domain([ 0, Math.max(1, d3.max(data, function(d)
  {
    return Math.round(d.replicationlag);
  })) ]).range([ height, 0 ]);

  valueline = d3.line().x(function(d)
  {
    return xScale(d.date);
  }).y(function(d)
  {
    return yScale(d.replicationlag);
  });

  xAxisCall = d3.axisBottom().scale(xScale).tickFormat(multiFormat);;
  yAxisCall = d3.axisLeft().tickFormat(function(e)
  {
    if (Math.round(e) != e)
    {
      return;
    }
    return e;
  }).tickValues(d3.range(0, 120, 1)).scale(yScale);

  svg.append("g").attr("class", "x axis").attr("transform",
      "translate(0," + height + ")").call(xAxisCall);
  svg.append("g").attr("class", "y axis").call(yAxisCall);

//text label for the y axis
  svg.append("text")
      .attr("transform", "rotate(-90)")
      .attr("y", 0 - margin.left)
      .attr("x",0 - (height / 2))
      .attr("dy", "1em")
      .style("text-anchor", "middle")
      .text("Replication lag (s)");
  
  chart_displayed = true;
}

function updateChart()
{
  server_obj = find_lag_per_server(id);
  if (server_obj == null)
  {
    $("#div_graph").html("No data available");
  } 
  else
  {
    if (server_obj['lags'].length < 2)
    {
      $("#div_graph").html("Not enough data available");
    }  
    else
    {  
      $("#div_graph").html("");
      buildChart();
    }  
  }
  if (chart_displayed)
  {
    renderChart();
    var i;
  }  
}

function mongoCommand(hostname, command)
{
  $("#server_command_output").html("Waiting for results").css('color', '#494747');
  sendOverWebSocket("mongocommand", "command", {server: hostname, mongocommand: command});
}

function serverdetails(id_in, hostname)
{
  id = id_in;
  show_div($("#mainright_serverdetails"));
  $("#mainright_serverdetails").html("");
  if ($("#" + hostname + "_master").html() == "No")
  {  
    $("#mainright_serverdetails").html(
    `
      <div class='hrlabel' id='hrlabelgraph'>Replication lag ` + hostname + `</div>
      <hr>
      <div class='showhide' id='showhide_graph' onclick='showhide($(this), $("#div_graph"))'>-</div>
      <div id='div_graph'></div>
    `);
    chart_displayed = false;
    setTimeout(updateChart, 50); // flush dom before adding chart
  }  
  $("#mainright_serverdetails").html($("#mainright_serverdetails").html() +
  `    
      <div class='hrlabel' id='hrlabelcommands'>Commands on ` + hostname + `</div>
      <hr>
      <div id="server_commands_div">
        <div id="server_commands_leftdiv">
          <div class='leftbutton' id='' onclick='mongoCommand("` + hostname + `","hostInfo")'>hostInfo</div>
          <div class='leftbutton' id='' onclick='mongoCommand("` + hostname + `","serverStatus")'>serverStatus</div>
          <div class='leftbutton' id='' onclick='mongoCommand("` + hostname + `","printReplicationInfo")'>printReplicationInfo</div>
        </div>
        <div id="server_commands_rightdiv">
          <div id="command_output_label">Command output</div>
          <div id="server_command_output_wrapper">
            <div id="server_command_output"></div>
          </div>
        </div>
      </div>    
  `); 
  if ($("#" + hostname + "_master").html() == "No")
  {  
    $("#server_command_output_wrapper").css('height', 'calc(100vh - 585px)');
  }  
  else
  {
    $("#server_command_output_wrapper").css('height', 'calc(100vh - 200px)');
  }
}
