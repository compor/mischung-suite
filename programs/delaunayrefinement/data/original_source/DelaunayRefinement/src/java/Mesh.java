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

File: Mesh.java 
*/

package DelaunayRefinement.src.java;


import objects.graph.Edge;
import objects.graph.EdgeGraph;
import objects.graph.Node;

import java.io.FileInputStream;
import java.util.zip.GZIPInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Stack;


public class Mesh {
  protected static final HashMap<Element.Edge, Node<Element>> edge_map = new HashMap<Element.Edge, Node<Element>>();


  @SuppressWarnings("unchecked")
  public static HashSet<Node<Element>> getBad(EdgeGraph<Element, Element.Edge> mesh) {
    HashSet<Node<Element>> ret = new HashSet<Node<Element>>();
    for (Node node : mesh) {
      Element element = mesh.getNodeData(node);
      if (element.isBad()) {
        ret.add(node);
      }
    }
    return ret;
  }


  private static Scanner getScanner(String filename) throws Exception {
    try {
      return new Scanner(new GZIPInputStream(new FileInputStream(filename + ".gz")));
    } catch (FileNotFoundException _) {
      return new Scanner(new FileInputStream(filename));
    }
  }


  private Tuple[] readNodes(String filename) throws Exception {
    Scanner scanner = getScanner(filename + ".node");

    int ntups = scanner.nextInt();
    scanner.nextInt();
    scanner.nextInt();
    scanner.nextInt();

    Tuple[] tuples = new Tuple[ntups];
    for (int i = 0; i < ntups; i++) {
      int index = scanner.nextInt();
      double x = scanner.nextDouble();
      double y = scanner.nextDouble();
      scanner.nextDouble(); // z
      tuples[index] = new Tuple(x, y, 0);
    }

    return tuples;
  }


  private void readElements(EdgeGraph<Element, Element.Edge> mesh, String filename, Tuple[] tuples) throws Exception {
    Scanner scanner = getScanner(filename + ".ele");

    int nels = scanner.nextInt();
    scanner.nextInt();
    scanner.nextInt();
    Element[] elements = new Element[nels];
    for (int i = 0; i < nels; i++) {
      int index = scanner.nextInt();
      int n1 = scanner.nextInt();
      int n2 = scanner.nextInt();
      int n3 = scanner.nextInt();
      elements[index] = new Element(tuples[n1], tuples[n2], tuples[n3]);
      addElement(mesh, elements[index]);
    }
  }


  private void readPoly(EdgeGraph<Element, Element.Edge> mesh, String filename, Tuple[] tuples) throws Exception {
    Scanner scanner = getScanner(filename + ".poly");

    scanner.nextInt();
    scanner.nextInt();
    scanner.nextInt();
    scanner.nextInt();
    int nsegs = scanner.nextInt();
    scanner.nextInt();
    Element[] segments = new Element[nsegs];
    for (int i = 0; i < nsegs; i++) {
      int index = scanner.nextInt();
      int n1 = scanner.nextInt();
      int n2 = scanner.nextInt();
      scanner.nextInt();
      segments[index] = new Element(tuples[n1], tuples[n2]);
      addElement(mesh, segments[index]);
    }
  }


  // .poly contains the perimeter of the mesh; edges basically, which is why it
  // contains pairs of nodes
  public void read(EdgeGraph<Element, Element.Edge> mesh, String basename) throws Exception {
    Tuple[] tuples = readNodes(basename);
    readElements(mesh, basename, tuples);
    readPoly(mesh, basename, tuples);
  }


  protected Node<Element> addElement(EdgeGraph<Element, Element.Edge> mesh, Element element) {
    Node<Element> node = mesh.createNode(element);
    mesh.addNode(node);
    for (int i = 0; i < element.numEdges(); i++) {
      Element.Edge edge = element.getEdge(i);
      if (!edge_map.containsKey(edge)) {
        edge_map.put(edge, node);
      } else {
        Edge<Element.Edge> new_edge = mesh.createEdge(node, edge_map.get(edge), edge);
        mesh.addEdge(new_edge);
        edge_map.remove(edge);
      }
    }
    return node;
  }


  public static boolean verify(EdgeGraph<Element, Element.Edge> mesh) {
    // ensure consistency of elements
    for (Node<Element> node : mesh) {
      Element element = mesh.getNodeData(node);
      if (element.getDim() == 2) {
        if (mesh.getOutNeighbors(node).size() != 1) {
          System.out.println("-> Segment " + element + " has " + mesh.getOutNeighbors(node).size() + " relation(s)");
          return false;
        }
      } else if (element.getDim() == 3) {
        if (mesh.getOutNeighbors(node).size() != 3) {
          System.out.println("-> Triangle " + element + " has " + mesh.getOutNeighbors(node).size() + " relation(s)");
          return false;
        }
      } else {
        System.out.println("-> Figures with " + element.getDim() + " edges");
        return false;
      }
    }
    // ensure reachability
    Node<Element> start = mesh.getRandom();
    Stack<Node<Element>> remaining = new Stack<Node<Element>>();
    HashSet<Node<Element>> found = new HashSet<Node<Element>>();
    remaining.push(start);
    while (!remaining.isEmpty()) {
      Node<Element> node = remaining.pop();
      if (!found.contains(node)) {
        found.add(node);
        for (Node<Element> neighbor : mesh.getOutNeighbors(node)) {
          remaining.push(neighbor);
        }
      }
    }
    if (found.size() != mesh.getNumNodes()) {
      System.out.println("Not all elements are reachable");
      return false;
    }
    return true;
  }
}
