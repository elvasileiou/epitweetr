# Get path of properties files (written by shiny app)
get_properties_path <- function() file.path(conf$data_dir, "properties.json")

# Get path of properties files (written by search loop)
get_plans_path <- function() file.path(conf$data_dir, "topics.json")

# Get task path (written by detect pipeline)
get_tasks_path <- function() file.path(conf$data_dir, "tasks.json")

# Get default languages file path
get_default_available_languages_path <- function() system.file("extdata", "languages.xlsx", package = get_package_name())

# Get available languages file path (writter by shiny app)
get_available_languages_path <- function() {
  path <- paste(conf$data_dir, "languages.xlsx", sep = "/")
  if(!file.exists(path))
    path <- get_default_available_languages_path()
  path
}

# Get default topics file path
get_default_known_users_path <- function() system.file("extdata", "users.xlsx", package = get_package_name())

# Get topics file path either from user or package location (written by shiny app)
get_known_users_path <- function(data_dir = conf$data_dir) {
    users_path <- paste(data_dir, "users.xlsx", sep = "/")
    if(!file.exists(users_path))
      users_path <- get_default_known_users_path()
    return(users_path)
}

# Get default topics files path
get_default_topics_path <- function() system.file("extdata", "topics.xlsx", package = get_package_name())

# Get topics file path either from user or package location (written by shiny app)
get_topics_path <- function(data_dir = conf$data_dir) {
    topics_path <- paste(data_dir, "topics.xlsx", sep = "/")
    if(!file.exists(topics_path))
      topics_path <- get_default_topics_path()
    return(topics_path)
}

# Get default country files path
get_default_countries_path <- function() system.file("extdata", "countries.xlsx", package = get_package_name())

# Get countries file path either from user or package location (written by shiny app)
get_countries_path <- function(data_dir = conf$data_dir) {
    countries_path <- paste(data_dir, "countries.xlsx", sep = "/")
    if(!file.exists(countries_path))
      countries_path <- get_default_countries_path() 
    return(countries_path)
}

# Get default subscribers files path
get_default_subscribers_path <- function() system.file("extdata", "subscribers.xlsx", package = get_package_name())

# Get the path for default or user defined subscribed user file (written by shiny app)
get_subscribers_path <- function() {
  path <- paste(conf$data_dir, "subscribers.xlsx", sep = "/")
  if(!file.exists(path))
    path <- get_default_subscribers_path()
  path
}

# Get email template path
get_email_alert_template_path <- function() system.file("extdata", "mail.html", package = get_package_name())

# Get scala building tools (SBT) dependencies file
get_sbt_file_dep_path <- function() system.file("extdata", "sbt-deps.txt", package = get_package_name())

# Get JAR directory
get_jars_dest_path <- function() file.path(conf$data_dir, "jars")

# Get application JAR (embedded on package)
get_app_jar_path <- function() system.file("java", "ecdc-twitter-bundle_2.12-1.0.jar", package = get_package_name())

# Get hadoop home path for winutils
get_winutils_hadoop_home_path <- function() file.path(conf$data_dir, "hadoop")

# Get winutils path
get_winutils_path <- function() file.path(get_winutils_hadoop_home_path(), "bin", "winutils.exe") 

# Get JSON file name for alert on given date
get_alert_file <- function(date) {
  alert_folder <- file.path(conf$data_dir, "alerts")
  if(!file.exists(alert_folder)) dir.create(alert_folder)
  alert_folder <- file.path(alert_folder, strftime(date, format="%Y"))
  if(!file.exists(alert_folder)) dir.create(alert_folder)
  alert_file <- file.path(alert_folder, paste(strftime(date, format="%Y.%m.%d"), "-alerts.json", sep = ""))
}

# Get geonames txt file
get_geonames_txt_path <- function() {
  file.path(conf$data_dir, "geo", "allCountries.txt") 
}

# Get geonames index path
get_geonames_index_path <- function() {
  file.path(conf$data_dir, "geo", "all-geos.parquet.index") 
}

# Get languages index path 
get_lang_index_path <- function() {
  file.path(conf$data_dir, "geo", "lang_vectors.index") 
}

# Get language vectors path
get_lang_vectors_path <- function(code) {
  file.path(conf$data_dir, "languages", paste(code, ".txt.gz", sep = "")) 
}
# Get language model path
get_lang_model_path <- function(code) {
  file.path(conf$data_dir, "languages", paste(code, ".txt.gz.model", sep = "")) 
}
# Get language model stamp
get_lang_stamp_path <- function(code) {
  file.path(conf$data_dir, "languages", paste(code, ".txt.gz.stamp", sep = "")) 
}
