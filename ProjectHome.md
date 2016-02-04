# NOTICE #

---

The java-gearman-service is now being maintained on github (https://github.com/gearman/java-service).

<br>
<br>
<h1>Requirements</h1>
<hr />
<ul><li><b>Java SE 7</b>
</li><li>slf4j 1.6.4+<br>
<br>
<br>
<h1>About</h1>
<hr />
<i>Java Gearman Service</i> is an easy-to-use distributed network application framework implementing the gearman protocol used to farm out work to other machines or processes that are better suited to do the work. It allows you to do work in parallel, to load balance processing, and to call functions between languages. It can be used in a variety of applications, from high-availability web sites to the transport of database replication events. In other words, it is the nervous system for how distributed processing communicates. A few strong points about Gearman:</li></ul>

<ul><li><b>Open Source</b> - It's free! (in both meanings of the word) Gearman has an active open source community that is easy to get involved with if you need help or want to contribute.</li></ul>

<ul><li><b>Multi-language</b> - There are interfaces for a number of languages, and this list is growing. You also have the option to write heterogeneous applications with clients submitting work in one language and workers performing that work in another. Go to the <a href='http://gearman.org'>Gearman Home</a> for other languages.</li></ul>

<ul><li><b>Flexible</b> - You are not tied to any specific design pattern. You can quickly put together distributed applications using any model you choose, one of those options being Map/Reduce.</li></ul>

<ul><li><b>Fast</b> - Gearman has a simple protocol and interface with a low overhead</li></ul>

<ul><li><b>Embeddable</b> - Since Gearman is fast and lightweight, it is great for applications of all sizes. It is also easy to introduce into existing applications with minimal overhead.</li></ul>

<ul><li><b>No single point of failure</b> - Gearman can not only help scale systems, but can do it in a fault tolerant way.<br>
<br>
<br>
<h1>How Does Gearman Work</h1>
<hr />
A Gearman powered application consists of three parts: a client, a worker, and a job server. The client is responsible for creating a job to be run and sending it to a job server. The job server will find a suitable worker that can run the job and forwards the job on. The worker performs the work requested by the client and sends a response to the client through the job server. Gearman provides client and worker APIs that your applications call to talk with the Gearman job server so you don't need to deal with networking or mapping of jobs. Internally, the gearman client and worker APIs communicate with the job server using TCP sockets.</li></ul>

<img src='http://gearman.org/images/gearman_stack.png' />
<br>
<br>

<h1>Examples</h1>
<hr />
<ul><li><a href='https://code.google.com/p/java-gearman-service/source/browse/#svn%2Fexamples%2Fjava-gearman-service%2Ftrunk%2Fsrc%2Fmain%2Fjava%2Forg%2Fgearman%2Fexamples%2Fecho'>Echo</a></li></ul>

<br>
<br>

<h1>Any Questions?</h1>
<hr />
Feel free to ask them at the <a href='http://groups.google.com/group/gearman'>google group</a>