﻿SootBasic
========
关于 import，为了方便查看，每个 .java 文件的开头均具体引入了所需的具体类，而非 package.*，如需整体引入可自行修改。

01-HelloWorld
--------
使用 Soot 构建好 HelloWorld 类之后，提供了 4 种输出方式：
- 0：输出 Jimple 源码
- 1：使用 ASMbackend 输出 .class 文件
- 2：使用 Jasmin 输出 .class 文件
- 4：

02-AddAttributes
--------
第二部分

03-AddProfiling
--------
https://github.com/soot-oss/soot/wiki/Adding-profiling-instructions-to-applications
主要有两种实现方式。
直接在测试类当中增加count变量进行记录，并进行println输出（复杂）。
创建自己的counter类，在测试类中插入调用stmt（相对简单）。

BTW (sth. about Soot)

**Note:** Whole-program packs are not enabled by default. You have to state the `-w` option on Soot’s command line to enable them.

Jimple is Soot’s primary intermediate representation, a three-address code representing a simplified version of Java with only around 15 different kinds of statements.

java -jar soot.jar -f baf/jimple/... file 

If you use `–f dava` to decompile to Java please make sure that the file `<jre>/lib/jce.jar` is on Soot’s classpath.
