/*
 * Copyright (c) 2010-2019, sikuli.org, sikulix.com - MIT license
 */

package org.sikuli.ide;

import org.sikuli.script.support.RunTime;

public class Sikulix {

  public static void main(String[] args) {
    RunTime.start("IDE", args);
    System.setProperty("sikuli.IDE_should_run", "develop");
    SikulixIDE.main(args);
  }
}
