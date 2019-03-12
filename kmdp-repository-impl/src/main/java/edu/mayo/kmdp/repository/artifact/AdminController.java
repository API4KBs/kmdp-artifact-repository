package edu.mayo.kmdp.repository.artifact;

import edu.mayo.kmdp.repository.artifact.jcr.JcrDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

//@Controller
public class AdminController {

  @Autowired
  private JcrDao jcrDao;

  @RequestMapping(value = "/nuke", method = RequestMethod.DELETE)
  public void nuke() throws Exception {
    this.jcrDao.reset();
  }
}
