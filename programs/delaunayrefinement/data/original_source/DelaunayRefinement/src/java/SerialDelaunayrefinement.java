/*
Lonestar Benchmark Suite for irregular applications that exhibit 
amorphous data-parallelism.

Center for Grid and Distributed Computing
The University of Texas at Austin

Copyright (C) 2007, 2008, 2009 The University of Texas at Austin

Licensed under the Eclipse Public License, Version 1.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.eclipse.org/legal/epl-v10.html

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

File: SerialDelaunayrefinement.java 
*/

package DelaunayRefinement.src.java;


import objects.graph.Edge;
import objects.graph.EdgeGraph;
import objects.graph.Node;
import objects.graph.UndirectedEdgeGraph;
import util.Time;

import java.util.Stack;


public class SerialDelaunayrefinement {

  private static boolean isFirstRun = true;


  public static void main(String[] args) {
    long runtime, lasttime, mintime, run;

    runtime = 0;
    lasttime = Long.MAX_VALUE;
    mintime = Long.MAX_VALUE;
    run = 0;
    while (((run < 3) || (Math.abs(lasttime - runtime) * 64 > Math.min(lasttime, runtime))) && (run < 7)) {
      System.gc();
      System.gc();
      System.gc();
      System.gc();
      System.gc();
      runtime = run(args);
      if (runtime < mintime)
        mintime = runtime;
      run++;
    }
    System.err.println("minimum runtime: " + mintime + " ms");
    System.err.println("");
  }


  public static long run(String args[]) {
    if (isFirstRun) {
      System.err.println();
      System.err.println("Lonestar Benchmark Suite v2.1");
      System.err.println("Copyright (C) 2007, 2008, 2009 The University of Texas at Austin");
      System.err.println("http://iss.ices.utexas.edu/lonestar/");
      System.err.println();
      System.err.println("application: Delaunay Mesh Refinement (serial version)");
      System.err.println("Refines a Delaunay triangulation mesh such that no angle");
      System.err.println("in the mesh is less than 30 degrees");
      System.err.println("http://iss.ices.utexas.edu/lonestar/delaunayrefinement.html");
      System.err.println();
    }
    if (args.length < 1) {
      throw new Error("Arguments: <input file> [verify]");
    }
    EdgeGraph<Element, Element.Edge> mesh = new UndirectedEdgeGraph<Element, Element.Edge>();
    Stack<Node<Element>> worklist;
    try {
      new Mesh().read(mesh, args[0]);
    } catch (Exception e) {
      throw new Error(e);
    }
    worklist = new Stack<Node<Element>>();
    worklist.addAll(Mesh.getBad(mesh));
    Cavity cavity = new Cavity(mesh);
    if (isFirstRun) {
      System.err.println("configuration: " + mesh.getNumNodes() + " total triangles, " + worklist.size() + " bad triangles");
      System.err.println();
    }
    long id = Time.getNewTimeId();
    while (!worklist.isEmpty()) {
      Node<Element> bad_element = worklist.pop();
      if ((bad_element != null) && (mesh.containsNode(bad_element))) {
        cavity.initialize(bad_element);
        cavity.build();
        cavity.update();
        // remove the old data
        for (Node<Element> node : cavity.getPre().getNodes()) {
          mesh.removeNode(node);
        }
        // add new data
        for (Node<Element> node : cavity.getPost().getNodes()) {
          mesh.addNode(node);
        }
        for (Edge<Element.Edge> edge : cavity.getPost().getEdges()) {
          mesh.addEdge(edge);
        }
        worklist.addAll(cavity.getPost().newBad(mesh));
        if (mesh.containsNode(bad_element)) {
          worklist.add(bad_element);
        }
      }
    }
    long time = Time.elapsedTime(id);
    System.err.println("runtime: " + time + " ms");

    if (isFirstRun && (args.length > 1)) {
      verify(mesh);
    }

    isFirstRun = false;
    return time;
  }


  @SuppressWarnings("unchecked")
  public static void verify(Object res) {
    EdgeGraph<Element, Element.Edge> result = (EdgeGraph<Element, Element.Edge>) res;
    if (!Mesh.verify(result)) {
      throw new IllegalStateException("refinement failed.");
    }
    int size = Mesh.getBad(result).size();
    if (size != 0) {
      throw new IllegalStateException("refinement failed\n" + "still have " + size + " bad triangles left.\n");
    }
    System.out.println("OK");
  }
}
