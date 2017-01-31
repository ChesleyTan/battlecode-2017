package fuzzyTesting;

import fuzzywuzzy.*;
import static fuzzywuzzy.rules.FuzzyLogic.*;

public class FuzzyTesting {
  
  InputVariable archonDistance = new InputVariable("archonDistance");
  InputVariable soldierCount = new InputVariable("soldierCount");
  InputVariable lumberCount = new InputVariable("lumberCount");
  InputVariable gardenerCount = new InputVariable("gardenerCount");
  InputVariable treeDensity = new InputVariable("treeDensity");
  InputVariable tankCount = new InputVariable("tankCount");
  InputVariable treeCount = new InputVariable("treeCount");
  
  OutputVariable type = new OutputVariable("type");
  
  InputTerm fewUnits = new InputTerm("few", 0, 0, 1, 5, 1);
  InputTerm mediumUnits = new InputTerm ("medium", 4, 10, 18, 25, 1);
  InputTerm manyUnits = new InputTerm("many", 23, 26, Double.MAX_VALUE, Double.MAX_VALUE, 1);
  
  InputTerm archonNear = new InputTerm("near", 0,0, 20, 35, 1);
  InputTerm archonFar = new InputTerm("far", 30, 40, Double.MAX_VALUE, Double.MAX_VALUE, 1);
  
  InputTerm sparseTrees = new InputTerm("sparse", 0,0, 1, 3, 1);
  InputTerm denseTrees = new InputTerm("dense", 2, 2, 4, 5, 1);
  InputTerm packedTrees = new InputTerm("packed", 4.5, 6, Double.MAX_VALUE, Double.MAX_VALUE, 1);
  
  InputTerm fewTrees = new InputTerm("few", 0,0, 1, 2, 1);
  InputTerm mediumTrees = new InputTerm("medium", 1, 2, 3, 4, 1);
  InputTerm manyTres = new InputTerm("many", 4, 5, Double.MAX_VALUE, Double.MAX_VALUE, 1);
  
  OutputTerm soldier = new OutputTerm("soldier", 3);
  OutputTerm tank = new OutputTerm("tank", 1);
  OutputTerm scout = new OutputTerm("scout", 2);
  OutputTerm lumberjack = new OutputTerm("lumberjack", 4);
  OutputTerm tree = new OutputTerm("tree", 0);
  FuzzyEngine engine = new FuzzyEngine(
    and(archonDistance.is(archonNear), soldierCount.is(fewUnits)).then(type.set(soldier)),
    treeDensity.is(packedTrees).then(type.set(lumberjack)),
    and(archonDistance.is(archonNear), treeDensity.is(packedTrees), lumberCount.is(fewUnits)).then(type.set(lumberjack)),
    and(archonDistance.is(archonNear), treeDensity.is(denseTrees)).then(type.set(soldier)),
    and(lumberCount.is(fewUnits), treeDensity.is(denseTrees)).then(type.set(lumberjack)),
    treeCount.is(fewTrees).then(type.set(tree))
  );
  
  
}