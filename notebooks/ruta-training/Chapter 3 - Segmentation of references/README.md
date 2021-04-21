# Chapter 3 - Segmentation of references

The goal of this chapter is to develop a set of rules for segmenting scientific references
according to their [bibtex fields](https://en.wikipedia.org/wiki/BibTeX#Field_types).
The rules have to create annotations of a reduced set of four different types: Author, Title,
Date represent the main information for distinguishing publications. Additionally, Venue
represents all remaining parts of the reference like Booktitle or Volume. The annotation schema
is illustrated in the following using inlined xml elements for representing the corresponding
annotations:

---

<author>Katchalski-Katzir E, Shariv I, Eisenstein M, Friesem A, Aflalo C, 
Vakser I:</author> <title>Molecular surface recognition: determination 
of geometric fit between proteins and their ligands by correlation 
techniques.</title> <venue>Proc Natl Acad Sci USA</venue> <date>1992,
</date> <venue>89(6): 2195-2199.</venue>

---

This chapter provides an introduction on how multiple rules scripts can be developed in different phases 
using three sets of annotated documents (train, validation, test). The rules for each specific entity are 
developed and optimized in separate notebooks and are then combined in the last notebook to a simple pipeline.