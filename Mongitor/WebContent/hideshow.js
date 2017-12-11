/**
 * 
 */

function hide_all_divs()
{
  $("#mainright").hide();
  $("#mainright_credentials").hide();
  $("#mainright_servers").hide();
  $("#mainright_usermanual").hide();
  $("#mainright_exit").hide();
  $("#mainright_serverdetails").hide();
  $("#mainright_cluster").hide();
  $("#mainright_databases").hide();
  $("#mainright_databasedetails").hide();
  $("#mainright_collection").hide();
  $("#mainright_shard").hide();
}

// function to show/hide panels
function showhide($knop, $panel)
{
  $panel.slideToggle(500, function ()
  {
    //execute this after slideToggle is done
    //change button based on visibility of content div
    $knop.text(function ()
    {
      //change text based on condition
      return $panel.is(":visible") ? "-" : "+";
    });
  });
}

function show_div($div)
{
  show_div2($div, true);
}

function show_div2($div, push)
{
  if (!$div.is(":visible"))
  {
    hide_all_divs();
    $div.show();
    if (push)
    {     
      history.pushState({id: $div.attr('id')}, "");
      console.log("push: "+ $div.attr('id'));
    }  
  }
}