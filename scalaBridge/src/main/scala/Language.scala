package org.ecdc.twitter 

import demy.storage.{Storage, WriteMode, FSNode}
import demy.mllib.text.Word2VecApplier
import demy.util.{log => l, util}
import org.apache.spark.sql.{SparkSession, Column, DataFrame, Row, Dataset}
import org.apache.spark.sql.functions.{col, lit, udf}
import org.apache.spark.sql.types._
import org.apache.spark.ml.linalg.SQLDataTypes.VectorType 
import org.apache.spark.ml.linalg.{Vectors, DenseVector, Vector=>MLVector}
import org.apache.spark.ml.classification.{LinearSVC, LinearSVCModel}
import java.lang.reflect.Method
import Geonames.GeoLookup 

case class Language(name:String, code:String, vectorsPath:String) {
   val modelPath = s"$vectorsPath.model"
   val vectorsSnapshot = s"$vectorsPath.parquet"
   def getVectors()(implicit spark:SparkSession, storage:Storage) = { 
     import spark.implicits._
     spark.read.text(vectorsPath).as[String]
       .map(s => s.split(" "))
       .filter(a => a.size>2)
       .map(a => (a(0), Vectors.dense(a.drop(1).map(s => s.toDouble)), this.code))
   }

   def getVectorsDF()(implicit spark:SparkSession, storage:Storage) = {
    /*(if(reuseExisting) util.restoreCheckPoint(this.vectorsSnapshot) else None)
      .getOrElse{
        util.checkpoint(
          df =*/ this.getVectors().toDF("word", "vector", "lang")/*
          , path = this.vectorsSnapshot
          , reuseExisting = false
        )
      }*/
   }

   def getStopWords(implicit spark:SparkSession, storage:Storage) = {
     import spark.implicits._
     this.getVectors().map(_._1).take(200).toSet
   }
   def prepare(locationPath:String, dataPath:String):Unit = {
     
   }

   def getGeoLikehoodModel(geonames:Geonames)(implicit spark:SparkSession, storage:Storage) = {
      if(storage.getNode(modelPath).exists)
        LinearSVCModel.load(modelPath)
      else { 
        val langDico = this.getVectorsDF()
       
        val geoVectors = 
          langDico.geoLookup(Seq(col("word")), geonames, maxLevDistance= 0, minScore = 10)
            .where(col("geo_id").isNotNull)
            .select(lit(1.0).as("label"), col("vector").as("feature"))
            .limit(1000)
        val nonGeoVectors = 
          langDico.geoLookup(Seq(col("word")), geonames, maxLevDistance= 0, minScore = 3)
            .where(col("geo_id").isNull)
            .select(lit(0.0).as("label"), col("vector").as("feature"))
            .limit(10000)

        val model = 
          new LinearSVC()
            .setFeaturesCol("feature")
            .setLabelCol("label")
        val trainData = geoVectors.union(nonGeoVectors).cache
        val trained = model.fit(trainData)
        trainData.unpersist
        trained.save(modelPath)
        trained
      }
   }
 }

 object Language {
   implicit class AddLikehood(val ds: Dataset[_]) {
     def addLikehoods(languages:Seq[Language], geonames:Geonames, vectorsColNames:Seq[String], langCodeColNames:Seq[String], likehoodColNames:Seq[String])(implicit spark:SparkSession, storage:Storage) = {
       Language.addLikehoods(
         df = ds.toDF
         , languages = languages
         , geonames = geonames
         , vectorsColNames = vectorsColNames
         , langCodeColNames = langCodeColNames
         , likehoodColNames = likehoodColNames
       ) 
     }
   }
   def addLikehoods(df:DataFrame, languages:Seq[Language], geonames:Geonames, vectorsColNames:Seq[String], langCodeColNames:Seq[String], likehoodColNames:Seq[String])(implicit storage:Storage) = {
     implicit val spark = df.sparkSession
     val trainedModels = spark.sparkContext.broadcast(languages.map(l => (l.code -> l.getGeoLikehoodModel(geonames))).toMap)
     Some(
       df.rdd.mapPartitions{iter => 
         val rawPredict = 
           trainedModels.value.mapValues{model => 
             val method = model.getClass.getDeclaredMethod("predictRaw", classOf[MLVector])
             method.setAccessible(true) 
             (model, method)
           }

         iter.map{row => 
            Some(
              vectorsColNames.zip(langCodeColNames)
                .map{case (vectorsCol, langCodeCol) =>
                  Some((row.getAs[String](langCodeCol), row.getAs[Seq[Row]](vectorsCol).map(r => r.getAs[DenseVector]("vector"))))
                    .map{case(langCode, vectors) => 
                      rawPredict.get(langCode)
                        .map{case (model, method) =>
                          vectors.map{vector => 
                            if(vector == null)
                              0.0
                            else
                              Some(method.invoke(model, vector).asInstanceOf[DenseVector])
                                .map{scores =>
                                  val (yes, no) = (scores(1), scores(0))
                                  if(no>=0 && yes>=0) yes/(no + yes)
                                  else if(no>=0 && yes<=0) 0.5 - Math.atan(no-yes)/Math.PI
                                  else if(no<=0 && yes>=0) 0.5 + Math.atan(yes-no)/Math.PI
                                  else no/(no + yes)
                                }
                                .get
                          }
                        }
                        .getOrElse(null)
                    }
                    .get
                }
              )
              .map(scoreVectors => Row.fromSeq(row.toSeq ++ scoreVectors))
              .get
         }
       })
       .map(rdd => spark.createDataFrame(rdd, StructType(fields = df.schema.fields ++ likehoodColNames.map(colName => StructField(colName , ArrayType(DoubleType, true), true)))))
       .get
   }
} 
       


/*predictions.withColumn(probaColumnName, udf((rawPreds:Seq[MLVector]) =>{
                        val scores = rawPreds.map(rawPred => {
                        var preds = rawPred.toArray
                        if(preds.size!=2) throw new Exception("Score can only be calculated for binary class")
                        val Array(no, yes) = preds

                        // Return Platt's score if A > 0, otherwise approximation
                        if (A > 1e-20)
                            1.0 / (1 + exp(A * no + B))
                        else {
                            if(no>=0 && yes>=0) yes/(no + yes)
                            else if(no>=0 && yes<=0) 0.5 - Math.atan(no-yes)/Math.PI
                            else if(no<=0 && yes>=0) 0.5 + Math.atan(yes-no)/Math.PI
                            else no/(no + yes)
                        }
                    })
                    scores.toArray
//        1
        } ).apply(array(col(rawScoresColumnName))))
*/

      
/*     val vectoriser = word2VecApplier  =
      new Word2VecApplier()
       .setInputCol("")
       .setOutputCol("docVector")
       .setFormat("text")
       .setVectorsPath(vectors)
       .setIndexPath(s"$dataFolder")
       .setReuseIndexFile(true)
       .setAccentSensitive(false)
       .setCaseSensitive(true)
       .setLogMetrics(false)
       .setSumWords(false)


  def vectorIndex(spark:SparkSession)  =
      word2VecApplier
        .setSumWords(true)
        .toIndex(spark)


  def inputDF(spark:SparkSession) = {
    (
      this.readFormat match {
        case "csv" =>
          spark.read
            .option("sep", this.csvSeparator)
            .option("header", "true")
            .option("quote", this.csvQuote)
            .option("escape", this.csvEscape)
            .option("charset", this.csvCharset)
            .option("multiLine", this.csvMultiLine)
            .csv(feedbackPath)
        case "pubmed" =>
          pubmed.PubmedDir(feedbackPath).getFlattenDS(spark = spark)
        case "parquet" =>
          spark.read.parquet(feedbackPath)
        case _ => throw new Exception(s"unvalid format $readFormat has been provided")
      }
    ).select((
      Seq(concat(
        this.readColumns.map(c =>
            concat(
              coalesce(col(c), lit(""))
              , if(addColumnNameOnText)
                  when(
                    col(c).isNull || trim(col(c)) === lit("")
                    ,lit(" ")
                  ).otherwise(lit(s" $c "))
                else
                  lit(" ")
            )):_*).as("text")
      ) ++ (validationTagsCol match {
        case Some(valCol) => Seq(split(col(valCol), validationTagsSep).as(valCol))
        case _ => Seq[Column]()
      }) ++ (idColumn match {
        case Some(id) => Seq(col(id).as("id"))
        case None => Seq(lit(None.asInstanceOf[Option[String]]).as("id"))
      })
    ) :_*
    ).where(trim(col("text"))=!="")
  }*/
