# Unit 1 - Endpoints in clinical trails

The goal of this unit is to create an prototypical application for extracting endpoint information from
pubmed abstracts about clinical trials. Further information about clinical endpoints in general
can be found in the [wikipedia article](https://en.wikipedia.org/wiki/Clinical_endpoint).
The targeted endpoints are restricted three main types with different manifestations: overall
response rate (ORR), overall survival (OS) and progression free survival (PFS). The latter two
are further distinguished in the median value, the time interval and its rate. The following
table gives an overview over the annotations types that should be identified:

| Name           | UIMA Type           | Example (annotation given in brackets)  |
| ------------- |:-------------:| -----:|
| ORR      | org.apache.uima.trials.type.ORR | The overall response rate (ORR) was [60%]. |
| median OS      | org.apache.uima.trials.type.OSMean   |   ... median OS was [12 months]. |
| OS time span | org.apache.uima.trials.type.OSTime     |    The [one-year] survival rate was 55.8%.|

  <table frame='all'>
      <tbody>
        <row>
          <entry>OS rate</entry>
          <entry>org.apache.uima.trials.type.OSRate</entry>
          <entry>The one-year survival rate was [55.8%].</entry>
        </row>
        <row>
          <entry>median PFS</entry>
          <entry>org.apache.uima.trials.type.PFSMean</entry>
          <entry>The median progression-free survival (PFS) was [10 months].</entry>
        </row>
        <row>
          <entry>PFS time span</entry>
          <entry>org.apache.uima.trials.type.PFSTime</entry>
          <entry>[12-month] PFS was 14.8%.</entry>
        </row>
        <row>
          <entry>PFS rate</entry>
          <entry>org.apache.uima.trials.type.PFSRate</entry>
          <entry>12-month PFS was [14.8%].</entry>
        </row>
      </tbody>
    </tgroup>
  </table>


  