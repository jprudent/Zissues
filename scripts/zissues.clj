
(ns zissues)
;; -- requirements

(require '[babashka.curl :as curl])
(require '[cheshire.core :as json])
(require '[hiccup2.core :as html])
(require '[hiccup.util :as html-util])
(require '[clojure.string :as str])
(require '[babashka.fs :as fs])
(import 'java.time.format.DateTimeFormatter
        'java.time.LocalDateTime
        'java.util.Base64)
(require '[babashka.deps :as deps])
(deps/add-deps '{:deps {markdown-clj/markdown-clj {:mvn/version "1.11.3"}
                        org.babashka/cli {:mvn/version "0.4.37"}}})
(require '[markdown.core :as md])
(require '[babashka.cli :as cli])

;; -- export to html

(defn get-a-page-of-issues [url]
      (println "Dumping url:" url)
      (curl/get url {:headers {"Accept" "vnd.github.v3+json"}}))

(defn next-nav-link
      [link-header]
      (some identity
            (map #(second (re-matches #"<(http[^>]+)>; rel=\"next\"" %))
                 (str/split link-header #", "))))

(defn get-all-issues
      [issues-url]
      (reduce into []
              (iteration get-a-page-of-issues
                         :initk issues-url
                         :vf #(json/parse-string (:body %) keyword)
                         :kf #(next-nav-link (get (:headers %) "link")))))

(defonce get-all-issues-cached (memoize get-all-issues))

(defn encode-b64 [bytes] (-> (Base64/getEncoder) (.encodeToString bytes)))
(defn image-src-to-b64 [src]
      (println "Embedding image" src)
      (str "data:image/" (fs/extension src) ";base64,"
           (encode-b64 (:body (curl/get src {:as :bytes})))))

(defonce image-src-to-b64-cached (memoize image-src-to-b64))

(defn embed-images
      [html]
      (let [srcs (re-seq #"https://user-images.githubusercontent.com[^\"']+" html)]
           (reduce (fn [acc src]
                       (str/replace-first acc src (image-src-to-b64-cached src)))
                   html srcs)))

(defn make-page-content
      [issue]
      [:article
       [:header
        [:h1 (:title issue)]
        [:div [:a {:id (:number issue), :href (:html_url issue)} "Go to original content"]]
        [:nav [:ul (map (fn [label] [:li [:a {:href (str/replace-first (:url label) "api.github.com/repos" "github.com")} (:name label)]]) (:labels issue))]]]
       (html-util/raw-string (embed-images (md/md-to-html-string (:body issue))))
       [:footer [:p (or (str "last edit on " (:updated_at issue)) (:created_at issue))]]])

(defn make-toc-entry
      [issue]
      [:li [:a {:href (str "#" (:number issue))} (:title issue)]])

(def date (LocalDateTime/now))
(def formatter (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss"))
(def date-str (.format date formatter))

(defn export-html
      [{:keys [repo output-file]}]
      (let [all-issues (get-all-issues-cached (str "https://api.github.com/repos/" repo "/issues"))
            nav [:aside [:nav [:ul (map make-toc-entry all-issues)]]]
            articles (map make-page-content all-issues)
            html-file (html/html [:html
                                  [:head
                                   [:link {:rel "stylesheet" :href "https://unpkg.com/@picocss/pico@latest/css/pico.min.css"}]
                                   [:title "Jerome issues backup"]
                                   [:meta {:generated date-str}]]
                                  [:body {:style "width: 900px"}
                                   nav
                                   articles]])]
           (spit output-file html-file)))

;; -- CLI

(def spec {:repo {:desc "Required. The github repository. username/repositoryname"
                  :require true}
           :output-file {:desc "The output file."
                         :default *out*
                         :default-desc "The standard output"}})

(defn error [& [fn & args]]
      (apply fn args)
      (System/exit 1))

(defn help []
      (println "usage: zissues <action> [options]")
      (println "example: zissues export-to-html --repo-url jprudent/jprudent.github.com")
      (println "  There is only one action:")
      (println "    - export-to-html: export the repository as a self contained HTML file")
      (println (cli/format-opts {:spec spec :order [:repo-url]})))

(defn error-fn [{:keys [msg]}]
      (println msg)
      (help))

(defn -main
      [& [action & args]]
      (when (not action) (error help))
      (let [opts (cli/parse-opts args {:spec spec
                                       :error-fn #(error error-fn %)  })]
           (case action
                 "export-to-html" (export-html opts)
                 (error help))))