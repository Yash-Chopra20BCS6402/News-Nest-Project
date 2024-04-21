import sys
import pandas as pd
import plotly.graph_objects as go
from textblob import TextBlob
from wordcloud import WordCloud
from nltk.tokenize import word_tokenize
from nltk.corpus import stopwords
from nltk.stem import WordNetLemmatizer
import nltk
import plotly.express as px
import gzip

nltk.download('punkt')
nltk.download('stopwords')
nltk.download('wordnet')

def load_data(data_list):
    df = pd.DataFrame(data_list)
    return df

def get_data_for_plot(data):
    category_counts = data['category'].value_counts()
    return category_counts

def plot_data(data, plots=None):
    plots_html = []  # List to store HTML representations of plots

    category_counts = get_data_for_plot(data)
    top_6_categories = category_counts.head(6)
    sentiment_scores = data['content'].apply(lambda x: TextBlob(x).sentiment.polarity)
    text = ' '.join(data['content'])
    wordcloud = WordCloud(width=800, height=400, background_color='white').generate(text)
    author_category_counts = data.groupby(['publisher_name', 'category']).size().unstack(fill_value=0)
    sentiment_distribution = pd.cut(sentiment_scores, bins=5, labels=['Very Negative', 'Negative', 'Neutral', 'Positive', 'Very Positive']).value_counts()
    summaries = text_summarization(data)

    for plot in plots:
        if plot == 'Category Counts':
            fig = go.Figure(go.Bar(x=category_counts.index, y=category_counts.values, marker_color='lightblue'))
            fig.update_layout(title='Category Counts', xaxis_title='Category', yaxis_title='Count')
            plots_html.append(fig.to_html(full_html=False, include_plotlyjs='cdn'))

        elif plot == 'Sentiment Analysis':
            fig = go.Figure(data=[go.Histogram(x=sentiment_scores, marker_color='lightgreen')])
            fig.update_layout(title='Sentiment Analysis', xaxis_title='Sentiment Polarity', yaxis_title='Frequency')
            plots_html.append(fig.to_html(full_html=False, include_plotlyjs='cdn'))

        elif plot == 'Word Cloud':
            fig = go.Figure(go.Image(z=wordcloud.to_array()))
            fig.update_layout(title='Word Cloud')
            plots_html.append(fig.to_html(full_html=False, include_plotlyjs='cdn'))

        elif plot == 'Author-wise Distribution of News Categories':
            fig = go.Figure()
            colors = px.colors.qualitative.Pastel
            for i, col in enumerate(author_category_counts.columns):
                fig.add_trace(go.Bar(x=author_category_counts.index, y=author_category_counts[col], name=col, marker_color=colors[i % len(colors)]))
            fig.update_layout(barmode='stack', title='Author-wise Distribution of News Categories', xaxis_title='Author', yaxis_title='Count')
            plots_html.append(fig.to_html(full_html=False, include_plotlyjs='cdn'))

        elif plot == 'Sentiment Distribution':
            fig = go.Figure(go.Bar(x=sentiment_distribution.index, y=sentiment_distribution.values, marker_color='lightskyblue'))
            fig.update_layout(title='Sentiment Distribution', xaxis_title='Sentiment', yaxis_title='Count')
            plots_html.append(fig.to_html(full_html=False, include_plotlyjs='cdn'))

        elif plot == 'Summaries':
            plots_html.append(display_summaries_table(data))

        elif plot == 'Category Distribution':
            fig = go.Figure(data=[go.Pie(labels=top_6_categories.index, values=top_6_categories.values, marker_colors=px.colors.qualitative.Pastel)])
            fig.update_layout(title='Category Distribution')
            plots_html.append(fig.to_html(full_html=False, include_plotlyjs='cdn'))

        elif plot == 'Average Title Length by Category':
            average_title_length = data.groupby('category')['title'].apply(lambda x: x.str.len().mean()).reset_index()
            fig = go.Figure(go.Bar(x=average_title_length['category'], y=average_title_length['title'], marker_color='lightseagreen'))
            fig.update_layout(title='Average Title Length by Category', xaxis_title='Category', yaxis_title='Average Length')
            plots_html.append(fig.to_html(full_html=False, include_plotlyjs='cdn'))

        elif plot == 'Number of Articles per Publisher':
            publisher_counts = data['publisher_name'].value_counts().reset_index()
            fig = go.Figure(go.Bar(x=publisher_counts['publisher_name'], y=publisher_counts['publisher_name'], marker_color='lightpink'))
            fig.update_layout(title='Number of Articles per Publisher', xaxis_title='Publisher', yaxis_title='Count')
            plots_html.append(fig.to_html(full_html=False, include_plotlyjs='cdn'))

        elif plot == 'Top N Most Common Words in Content':
            plots_html.append(top_n_common_words(data['content'], 10, 'Top 10 Most Common Words in Content', color='lightblue'))

        elif plot == 'Top N Most Common Words in Publisher Names':
            plots_html.append(top_n_common_words(data['publisher_name'], 10, 'Top 10 Most Common Words in Publisher Names', color='lightgreen'))

        elif plot == 'Top N Most Common Words in Titles':
            plots_html.append(top_n_common_words(data['title'], 10, 'Top 10 Most Common Words in Titles', color='lightskyblue'))

    return ''.join(plots_html)  # Concatenate HTML representations of plots

def preprocess_text(text):
    stop_words = set(stopwords.words('english'))
    lemmatizer = WordNetLemmatizer()
    tokens = word_tokenize(text.lower())  # Convert text to lowercase and tokenize
    words = [lemmatizer.lemmatize(word) for word in tokens if word.isalpha() and word not in stop_words]
    return words

def text_summarization(data):
    summaries = data['content'].apply(lambda x: ' '.join(preprocess_text(x)))
    summaries.to_csv('D:\\Users\\DELL\\Documents\\GitHub\\news-nest\\app\\Spark\\text_summaries.csv', index=True)
    return summaries

def display_summaries_table(data, page=1, items_per_page=25):
    start_index = (page - 1) * items_per_page
    end_index = start_index + items_per_page
    df = pd.DataFrame(data)
    table_html = df.iloc[start_index:end_index].to_html(index=False, classes='table', justify='center')
    pagination_dropdown = generate_pagination_dropdown(len(df), page)
    return table_html + pagination_dropdown

def generate_pagination_dropdown(total_items, current_page):
    total_pages = (total_items + 24) // 25  # 25 items per page
    options = ''.join([f'<option value="{i}" {"selected" if i == current_page else ""}>{i}</option>' for i in range(1, total_pages + 1)])
    dropdown = f'<div style="margin-top: 10px;">' \
               f'   <label for="page">Page:</label>' \
               f'   <select id="page" onchange="changePage(this.value)">' \
               f'       {options}' \
               f'   </select>' \
               f'</div>' \
               f'<script>' \
               f'   function changePage(page) {{ window.location.href = `?page=${{page}}`; }}' \
               f'</script>'
    return dropdown

def top_n_common_words(data_column, n, plot_title, color='lightcoral'):
    words_list = []
    for text in data_column:
        words_list.extend(preprocess_text(text))
    word_freq = nltk.FreqDist(words_list)
    most_common_words = word_freq.most_common(n)
    words, freq = zip(*most_common_words)
    fig = go.Figure(go.Bar(x=list(words), y=list(freq), marker_color=color))
    fig.update_layout(title=plot_title, xaxis_title='Word', yaxis_title='Frequency')
    return fig.to_html(full_html=False, include_plotlyjs='cdn')

def main(data_list, plots):
    data = load_data(data_list)
    plots_html = plot_data(data, plots)
    generate_html_file(plots_html)

def generate_html_file(html_content):
    with gzip.open('plots.html.gz', 'wt') as f:
        f.write(html_content)
    print("Plots generated successfully. Open 'plots.html.gz' in your browser.")

if __name__ == "__main__":
    data_str = sys.stdin.read()
    data_list = eval(data_str)
    plots = ['Category Counts', 'Sentiment Analysis', 'Word Cloud', 'Author-wise Distribution of News Categories',
             'Sentiment Distribution', 'Summaries', 'Category Distribution', 'Average Title Length by Category',
             'Number of Articles per Publisher', 'Top N Most Common Words in Content',
             'Top N Most Common Words in Publisher Names', 'Top N Most Common Words in Titles']
    main(data_list, plots)
