/**
 * 
 */
function show_pie(data)
{
  //var data = [10, 20, 100];

  var width = 200,
      height = 200,
      radius = Math.min(width, height) / 2;

  var color = d3.scaleOrdinal()
      .range(["#4D4D4D", "#5DA5DA", "#FAA43A", "#60BD68", "#F17CB0", "#B2912F", "#B276B", "#DECF3F", "#F15854"]);

  var arc = d3.arc()
      .outerRadius(radius - 10)
      .innerRadius(0);

  var labelArc = d3.arc()
      .outerRadius(radius - 40)
      .innerRadius(radius - 40);

  var pie = d3.pie()
      .sort(null)
      .value(function(d) { return d.size; });

  var svg = d3.select("#div_shard_distribution").append("svg")
      .attr("width", width)
      .attr("height", height)
    .append("g")
      .attr("transform", "translate(" + width / 2 + "," + height / 2 + ")");

    var g = svg.selectAll(".arc")
        .data(pie(data))
      .enter().append("g")
        .attr("class", "arc");

    g.append("path")
        .attr("d", arc)
        .style("fill", function(d) { return color(d.data.shard); });

    g.append("text")
        .attr("transform", function(d) { return "translate(" + labelArc.centroid(d) + ")"; })
        .attr("dy", ".35em")
        .text(function(d) { return d.data.shard; });
  

}