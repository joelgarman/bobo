package com.browseengine.bobo.facets.filter;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;

public class AndDocIdSet extends DocIdSet{
  private List<DocIdSet> sets = null;
  private int nonNullSize; // excludes nulls
  
  public AndDocIdSet(List<DocIdSet> docSets) {
    this.sets = docSets;
    int size = 0;
    if (sets != null) {
      for(DocIdSet set : sets) {
        if(set != null) size++;
      }
    }
    nonNullSize = size;
  }
  
  class AndDocIdSetIterator extends DocIdSetIterator {
    int lastReturn = -1;
    private DocIdSetIterator[] iterators = null;

    AndDocIdSetIterator() throws IOException{
      if (nonNullSize < 1)
        throw new IllegalArgumentException("Minimum one iterator required");
      
      iterators = new DocIdSetIterator[nonNullSize];
      int j = 0;
      for (DocIdSet set : sets) {
        if (set != null) {
          DocIdSetIterator dcit = set.iterator();
          if(dcit == null)
            dcit = DocIdSet.EMPTY_DOCIDSET.iterator();
          iterators[j++] = dcit;
        }
      }
      lastReturn = (iterators.length > 0 ? -1 : DocIdSetIterator.NO_MORE_DOCS);
    }

    @Override
    public final int docID() {
      return lastReturn;
    }
    
    @Override
    public final int nextDoc() throws IOException {
      
      if (lastReturn == DocIdSetIterator.NO_MORE_DOCS) return DocIdSetIterator.NO_MORE_DOCS;
      
      DocIdSetIterator dcit = iterators[0];
      int target = dcit.nextDoc();
      int size = iterators.length;
      int skip = 0;
      int i = 1;
      while (i < size) {
        if (i != skip) {
          dcit = iterators[i];
          int docid = dcit.advance(target);
          
          if (docid > target) {
            target = docid;
            if(i != 0) {
              skip = i;
              i = 0;
              continue;
            }
            else
              skip = 0;
          }
        }
        i++;
      }
//      if(target != DocIdSetIterator.NO_MORE_DOCS)
//        _interSectionResult.add(target);
      return (lastReturn = target);
    }


    
    @Override
    public final int advance(int target) throws IOException {

      if (lastReturn == DocIdSetIterator.NO_MORE_DOCS) return DocIdSetIterator.NO_MORE_DOCS;
      
      DocIdSetIterator dcit = iterators[0];
      target = dcit.advance(target);
      int size = iterators.length;
      int skip = 0;
      int i = 1;
      while (i < size) {
        if (i != skip) {
          dcit = iterators[i];
          int docid = dcit.advance(target);
          if (docid > target) {
            target = docid;
            if(i != 0) {
              skip = i;
              i = 0;
              continue;
            }
            else
              skip = 0;
          }
        }
        i++;
      }
      return (lastReturn = target);
    }
  }

  public final DocIdSetIterator iterator() throws IOException{
    return new AndDocIdSetIterator();
  }
}
