package com.starkandwayne.springcloudconfigserver.dashboard;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
public class DashboardController {

   
   @RequestMapping(path = "/dashboard")
   public String index() {

       return "Dashboard Not Yet Implemented";
   }
}