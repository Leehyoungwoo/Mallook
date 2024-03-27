# hiver 상품 상세정보 크롤링(API)
import requests
import time
import json
import pickle
import os
from fake_useragent import UserAgent
from bs4 import BeautifulSoup
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.common.keys import Keys
from multiprocessing import Pool
from pymongo import MongoClient
from dotenv import load_dotenv

password = os.getenv("MONGODB_PASSWORD")
API_KEY = os.getenv("API_KEY")

client = MongoClient(f'mongodb+srv://root:{password}@cluster0.stojj99.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0')
db = client.hiver

# Chrome 옵션 설정
options = Options()
options.add_experimental_option("detach", True)

user_agent = UserAgent().random
options.add_argument(f'user-agent={user_agent}')
options.add_argument("disable-gpu") 
options.add_argument("disable-infobars")
options.add_argument("--disable-extensions")
options.add_argument('--disable-images')
# options.add_argument('--headless')

# 속도 향상을 위한 옵션 해제
prefs = {'profile.default_content_setting_values': {
    # 'cookies' : 2, 
    'images': 2, 
    'plugins' : 2, 
    'popups': 2, 
    'geolocation': 2, 
    'notifications' : 2, 
    'auto_select_certificate': 2, 
    'fullscreen' : 2, 
    'mouselock' : 2, 
    'mixed_script': 2, 
    'media_stream' : 2, 
    'media_stream_mic' : 2, 
    'media_stream_camera': 2, 
    'protocol_handlers' : 2, 
    'ppapi_broker' : 2, 
    'automatic_downloads': 2, 
    'midi_sysex' : 2, 
    'push_messaging' : 2, 
    'ssl_cert_decisions': 2, 
    'metro_switch_to_desktop' : 2, 
    'protected_media_identifier': 2, 
    'app_banner': 2, 
    'site_engagement' : 2, 
    'durable_storage' : 2
    }
}   

# options.add_experimental_option('prefs', prefs)

# 웹 드라이버 로드
driver = webdriver.Chrome(options=options)

# 카테고리 번호
with open("HiverCategory.pkl", "rb") as f:
    category_numbers = pickle.load(f)

size_names = {
    '사이즈', 'SIZE', 'size', 'Size', '사이즈1', 'size1', 'SIZE1', 'Size1', '',
}

# 상품 목록들(productId 기준)
hiver_products = {}
# 중복을 제거하기 위한 상품 코드들
hiver_used = set()

# 상품 상세정보를 받아오는 함수
def hiver_process(category_info):
    # 분류
    main_categories, sub_categories, category_number = category_info
    # category_url = f'https://capi.hiver.co.kr/v1/web/categories/{category_number}/products'
    
    # 리뷰 파라미터
    photo_params = {
        'is-first': 'false',
        'tab-type': 'photo',
        'limit': 100,
        'offset': 0,
        'version': 2101,
        'service-type': 'hiver',
    }

    text_params = {
        'is-first': 'false',
        'tab-type': 'text',
        'limit': 100,
        'offset': 0,
        'version': 2101,
        'service-type': 'hiver',
    }

    # # 한 번호당 5000개까지 조회가능
    # for offset in range(0, 5001, 100):
    #     # 0, 100, ~, 4999
    #     if offset == 5000:
    #         offset = offset - 1

    # API 요청을 위한 헤더와 파라미터
    headers = {
        'Authorization': API_KEY,
        'User-Agent': UserAgent().random,
    }

    headers2 = {
        'User-Agent': UserAgent().random,
    }

        # params = {
        #     'offset': offset,
        #     'limit': 100,
        #     'order': 'popular',
        #     'type': 'all',
        #     'service-type': 'hiver',
        # }
        
        # # 카테고리 API 요청
        # category_response = requests.get(category_url, headers=headers, params=params)
        # category_data = category_response.json()
    
    product = {
        'id': "96342356",
    }
    # 사용되지 않은 프로덕트라면 세부정보 저장
    if product['id'] not in hiver_used:
        print(f'[{product["id"]}]', main_categories, sub_categories, category_number)
        start_time = time.time()

        # 중복 기록
        hiver_used.add(product['id'])
        
        # 상품 세부 정보 불러오기
        product_url = f'https://www.hiver.co.kr/_next/data/NkOT5yhyYbV_Xxvt8xp9e/ko/products/{product["id"]}.json'
        # 상품 API 요청
        product_response = requests.get(product_url, headers=headers)
        product_data = product_response.json()

        # 이미지 파싱 및 리스트에 담기
        complexity = f'@\"/products/{{id}}\",\"{product["id"]}\",'
        image_code = product_data['pageProps']['fallback'][complexity]['data']['text']
        soup = BeautifulSoup(image_code, 'html.parser')
        image_urls = []
        image_tags = soup.find_all('img')
        for tag in image_tags:
            src = tag.get('src')
            if src:
                image_urls.append(src)

        reviews = {
            'count': 0,
            'reviews': []
        }

        print(f'[{product["id"]}]', '### 리뷰정보 조회 ###')

        # 리뷰 요청
        review_url = f'https://hiver-api.brandi.biz/v2/web/products/{product["id"]}/reviews'
        # 한 번호당 5000개까지 조회가능
        for offset2 in range(0, 5001, 100):
            # 0, 100, ~, 4999
            if offset2 == 5000:
                offset2 = offset2 - 1

            # 포토 리뷰 파라미터
            photo_params = {
                'is-first': 'false',
                'tab-type': 'photo',
                'limit': 100,
                'offset': offset2,
                'version': 2101,
                'service-type': 'hiver',
            }

            # 포토 리뷰 API 요청
            photo_review_response = requests.get(review_url, headers=headers2, params=photo_params)
            photo_review_data = photo_review_response.json()

            # 리뷰 데이터가 있는 경우에 넣기
            if photo_review_data['data']:
                reviews['count'] += photo_review_data['meta']['count']
                for photo_review in photo_review_data['data']:
                    review = {
                        'content': photo_review['text'],
                        'created_at': photo_review['created_time'],
                        'images': photo_review['user']['image_url'],
                        'point': None,
                        'product_option': [photo_review['product']['option_name'].split('/')],
                        'user_size': [photo_review['user']['height'], photo_review['user']['weight']],
                    }

                    reviews['reviews'].append(review)
            # 데이터가 없는 경우 넘기기
            else:
                break
        
        # 한 번호당 5000개까지 조회가능
        for offset2 in range(0, 5001, 100):
            # 0, 100, ~, 4999
            if offset2 == 5000:
                offset2 = offset2 - 1

            text_params = {
                'is-first': 'false',
                'tab-type': 'text',
                'limit': 100,
                'offset': offset2,
                'version': 2101,
                'service-type': 'hiver',
            }

            # 텍스트 리뷰 API 요청
            text_review_response = requests.get(review_url, headers=headers2, params=text_params)
            text_review_data = text_review_response.json()

            # 리뷰 데이터가 있는 경우에 넣기
            if text_review_data['data']:
                reviews['count'] += text_review_data['meta']['count']
                for text_review in text_review_data['data']:
                    review = {
                        'content': text_review['text'],
                        'created_at': text_review['created_time'],
                        'images': None,
                        'point': None,
                        'product_option': [text_review['product']['option_name'].split('/')],
                        'user_size': [text_review['user']['height'], text_review['user']['weight']],
                    }

                    reviews['reviews'].append(review)
            # 데이터가 없는 경우 종료
            else:
                break
        
        # 태그 조회
        tag_list = []
        tags = product_data['pageProps']['fallback'][complexity]['data']['tags']
        for tag in tags:
            tag_list.append(tag['name'])

        print(f'[{product["id"]}]', '#### 색상/사이즈 조회 ####')

        # 웹 페이지 열기
        web_url = f'https://www.hiver.co.kr/products/{product["id"]}'
                    
        # 상품이 존재하는지 확인 (존재하지 않는다면 404 발생)
        try:
            response = requests.get(web_url)
            response.raise_for_status()
        except:
            print(f'[{product["id"]}] 조회 실패 Pass' )
            return

        driver.get(web_url)

        # 팝업버튼이 존재하면 클릭
        try:
            # 버튼 클릭하여 요소 로드 대기
            popup = driver.find_element(By.CSS_SELECTOR, 'button.textButton.btn-text.css-1hv5ygr')
            popup.send_keys(Keys.ENTER)
        except:
            pass
    
        # 암시적 대기 설정 (10초)
        driver.implicitly_wait(10)

        # 구매 버튼 클릭
        try:
            # 버튼 클릭하여 요소 로드 대기
            button = driver.find_element(By.CSS_SELECTOR, 'button.order.css-xnq7lu')
            button.send_keys(Keys.ENTER)
        except:
            print(f'[{product["id"]}] 버튼 클릭 불가 Pass')
            return

        # 색상 선택
        colors = driver.execute_script('''
            var names = [];
            var elements = document.querySelectorAll('p.name');
            elements.forEach(function(elem) {
                names.push(elem.textContent.trim());
            });
            return names;
        ''')

        # 사이즈 이름
        try:
            size_name = driver.execute_script(f'''
                var element = document.querySelector('details.product-option.css-zzmtgj:nth-child(2) p.title');
                var text = element.textContent.trim();
                return text;
            ''')
            print(size_name)
            # 사이즈를 나타내는 것이 아니라면 넘기기
            if size_name not in size_names:
                print('No size')
                return
        except:
            pass

        # 사이즈 선택
        try:
            sizes = driver.execute_script(f'''
                var sizeNames = [];
                var sizeElements = document.querySelectorAll('details.product-option.css-zzmtgj:nth-child(2) p.name');
                sizeElements.forEach(function(elem) {{
                    sizeNames.push(elem.textContent.trim());
                }});
                return sizeNames;
            ''')
        except:
            sizes = []

        # 버튼 리스트가 열려있지 않다면
        if not sizes:
            # 품절이 아닌 상품 클릭
            try:
                prod_list = driver.find_elements(By.CSS_SELECTOR, 'div.bottom-modal.modal-wrap.purchaseModal.css-2aucks.modal-open li')
                for prod in prod_list:
                    # 클릭 가능시 버튼 누르기
                    try:
                        prod.click()

                        # 사이즈 선택
                        sizes = driver.execute_script(f'''
                            var sizeNames = [];
                            var sizeElements = document.querySelectorAll('details.product-option.css-zzmtgj:nth-child(2) p.name');
                            sizeElements.forEach(function(elem) {{
                                sizeNames.push(elem.textContent.trim());
                            }});
                            return sizeNames;
                        ''')
                        break
                        
                    # 클릭이 안되면 다음 것 확인
                    except:
                        continue
                # 클릭할 수 있는게 없다면 이번 상품 건너뛰기
                else:
                    print(f'[{product["id"]}] 건너 뛰기')
                    return
                    

            # 상품 클릭이 안되면 넘기기
            except:
                print(f'[{product["id"]}] 색상 클릭 불가 Pass')
                return

        # print('### 상품 정보 입력 ###')
            
        # 카테고리 재분류
        if '분류' in sub_categories:
            if main_categories == '상의':
                if sub_categories == '후드티(분류필요)':
                    if '집업' in product_data['pageProps']['title']:
                        main_categories = '아우터'
                        sub_categories = '후드집업'
                    else:
                        sub_categories = '후드티'
            elif main_categories == '하의':
                if sub_categories == '기타(분류필요)':
                    if '조거' in product_data['pageProps']['title']:
                        sub_categories = '트레이닝/조거팬츠'
                    else:
                        sub_categories = '기타'
                elif sub_categories == '스키니(분류필요)':
                    sub_categories = '기타'
                elif sub_categories == '와이드(분류필요)':
                    sub_categories = '와이드'
            elif main_categories == '아우터':
                if sub_categories == '패딩(분류필요)':
                    if '숏' in product_data['pageProps']['title']:
                        sub_categories = '숏패딩/패딩조끼'
                    elif '조끼' in product_data['pageProps']['title']:
                        sub_categories = '숏패딩/패딩조끼'
                    else:
                        sub_categories = '롱패딩'
                elif sub_categories == '재킷(분류필요)':
                    if '무스탕' in product_data['pageProps']['title']:
                        sub_categories = '무스탕'
                    elif '플리스' in product_data['pageProps']['title']:
                        sub_categories = '플리스'
                    elif '후리스' in product_data['pageProps']['title']:
                        sub_categories = '후리스'
                    elif '바람' in product_data['pageProps']['title']:
                        sub_categories = '바람막이'
                    elif '윈드' in product_data['pageProps']['title']:
                        sub_categories = '바람막이'
                    elif '아노락' in product_data['pageProps']['title']:
                        sub_categories = '바람막이'
                    else:
                        sub_categories = '재킷'
                elif sub_categories == '점퍼(분류필요)':
                    if '무스탕' in product_data['pageProps']['title']:
                        sub_categories = '무스탕'
                    elif '플리스' in product_data['pageProps']['title']:
                        sub_categories = '플리스'
                    elif '후리스' in product_data['pageProps']['title']:
                        sub_categories = '후리스'
                    elif '바람' in product_data['pageProps']['title']:
                        sub_categories = '바람막이'
                    elif '윈드' in product_data['pageProps']['title']:
                        sub_categories = '바람막이'
                    elif '아노락' in product_data['pageProps']['title']:
                        sub_categories = '바람막이'
                    else:
                        sub_categories = '점퍼'
                elif sub_categories == '기타(분류필요)':
                    if '무스탕' in product_data['pageProps']['title']:
                        sub_categories = '무스탕'
                    elif '플리스' in product_data['pageProps']['title']:
                        sub_categories = '플리스'
                    elif '후리스' in product_data['pageProps']['title']:
                        sub_categories = '후리스'
                    elif '바람' in product_data['pageProps']['title']:
                        sub_categories = '바람막이'
                    elif '윈드' in product_data['pageProps']['title']:
                        sub_categories = '바람막이'
                    elif '아노락' in product_data['pageProps']['title']:
                        sub_categories = '바람막이'
                    else:
                        sub_categories = '기타'
                elif sub_categories == '베스트(분류필요)':
                    if '패딩' in product_data['pageProps']['title']:
                        sub_categories = '숏패딩/패딩조끼'
                    else:
                        sub_categories = '기타'
            elif main_categories == '가방':
                if sub_categories == '기타(분류필요)':
                    if '웨이스트' in product_data['pageProps']['title']:
                        sub_categories = '웨이스트백'
                    else:
                        sub_categories = '기타'
            elif main_categories == '신발':
                if sub_categories == '운동화(분류필요)':
                    if '러닝' in product_data['pageProps']['title']:
                        sub_categories = '러닝화/워킹화'
                    elif '워킹' in product_data['pageProps']['title']:
                        sub_categories = '러닝화/워킹화'
                    elif '축구' in product_data['pageProps']['title']:
                        sub_categories = '스포츠화'
                    elif '농구' in product_data['pageProps']['title']:
                        sub_categories = '스포츠화'
                    elif '테니스' in product_data['pageProps']['title']:
                        sub_categories = '스포츠화'
                    elif '골프' in product_data['pageProps']['title']:
                        sub_categories = '스포츠화'
                    else:
                        sub_categories == '러닝화/워킹화'
                elif sub_categories == '구두/로퍼(분류필요)':
                    if '로퍼' in product_data['pageProps']['title']:
                        sub_categories = '로퍼'
                    else:
                        sub_categories = '구두'
                elif sub_categories == '샌들/슬리퍼(분류필요)':
                    if '뮬' in product_data['pageProps']['title']:
                        sub_categories = '뮬/블로퍼'
                    elif '블로퍼' in product_data['pageProps']['title']:
                        sub_categories = '뮬/블로퍼'
                    else:
                        sub_categories = '샌들/슬리퍼'
            elif main_categories == '모자':
                if sub_categories == '캡(분류필요)':
                    if '스냅' in product_data['pageProps']['title']:
                        sub_categories = '스냅백'
                    else:
                        sub_categories = '볼캡/야구모자'
                elif sub_categories == '기타(분류필요)':
                    if '스냅백' in product_data['pageProps']['title']:
                        sub_categories = '스냅백'
                    elif '베레모' in product_data['pageProps']['title']:
                        sub_categories = '베레모'
                    elif '페도라' in product_data['pageProps']['title']:
                        sub_categories = '페도라'
                    elif '중절모' in product_data['pageProps']['title']:
                        sub_categories = '페도라'
                    else:
                        sub_categories = '기타'
            elif main_categories == '빅사이즈':
                if sub_categories == '티셔츠(분류필요)':
                    main_categories = '상의'
                    if '긴팔' in product_data['pageProps']['title']:
                        sub_categories = '긴팔티'
                    elif '반팔' in product_data['pageProps']['title']:
                        sub_categories = '반팔티'
                    else:
                        sub_categories = '기타'
                elif sub_categories == '맨투맨/후드(분류필요)':
                    main_categories = '상의'
                    if '후드' in product_data['pageProps']['title']:
                        sub_categories = '후드티'
                    else:
                        sub_categories = '맨투맨'
                elif sub_categories == '니트/가디건(분류필요)':
                    main_categories = '상의'
                    if '가디건' in product_data['pageProps']['title']:
                        sub_categories = '가디건'
                    else:
                        sub_categories = '니트/스웨터'
                elif sub_categories == '트레이닝복(분류필요)':
                    return
                elif sub_categories == '점퍼/야상/패딩(분류필요)':
                    main_categories = '아우터'
                    if '숏패딩' in product_data['pageProps']['title']:
                        sub_categories = '숏패딩/패딩조끼'
                    elif '롱패딩' in product_data['pageProps']['title']:
                        sub_categories = '롱패딩'
                    elif '무스탕' in product_data['pageProps']['title']:
                        sub_categories = '무스탕'
                    elif '플리스' in product_data['pageProps']['title']:
                        sub_categories = '플리스'
                    elif '후리스' in product_data['pageProps']['title']:
                        sub_categories = '플리스'
                    elif '바람' in product_data['pageProps']['title']:
                        sub_categories = '바람막이'
                    elif '윈드' in product_data['pageProps']['title']:
                        sub_categories = '바람막이'
                    elif '아노락' in product_data['pageProps']['title']:
                        sub_categories = '바람막이'
                    elif '점퍼' in product_data['pageProps']['title']:
                        sub_categories = '점퍼'
                    else:
                        sub_categories = '기타'
                elif sub_categories == '자켓/코트(분류필요)':
                    main_categories = '아우터'
                    if '숏코트' in product_data['pageProps']['title']:
                        sub_categories = '숏코트'
                    elif '롱코트' in product_data['pageProps']['title']:
                        sub_categories = '롱코트'
                    elif '라이더' in product_data['pageProps']['title']:
                        sub_categories = '라이더 재킷'
                    elif '무스탕' in product_data['pageProps']['title']:
                        sub_categories = '무스탕'
                    elif '플리스' in product_data['pageProps']['title']:
                        sub_categories = '플리스'
                    elif '후리스' in product_data['pageProps']['title']:
                        sub_categories = '플리스'
                    elif '바람' in product_data['pageProps']['title']:
                        sub_categories = '바람막이'
                    elif '윈드' in product_data['pageProps']['title']:
                        sub_categories = '바람막이'
                    elif '아노락' in product_data['pageProps']['title']:
                        sub_categories = '바람막이'
                    else:
                        sub_categories = '재킷'
                elif sub_categories == '팬츠(분류필요)':
                    main_categories = '하의'
                    if '데님' in product_data['pageProps']['title']:
                        sub_categories = '데님'
                    elif '면' in product_data['pageProps']['title']:
                        sub_categories = '면'
                    elif '슬랙스' in product_data['pageProps']['title']:
                        sub_categories = '슬랙스'
                    elif '트레이닝' in product_data['pageProps']['title']:
                        sub_categories = '트레이닝/조거팬츠'
                    elif '숏' in product_data['pageProps']['title']:
                        sub_categories = '숏팬츠'
                    elif '반바지' in product_data['pageProps']['title']:
                        sub_categories = '숏팬츠'
                    else:
                        sub_categories = '기타'
            elif main_categories == '럭셔리':
                main_categories = '상의'
                if '긴팔' in product_data['pageProps']['title']:
                    sub_categories = '긴팔티'
                elif '반팔' in product_data['pageProps']['title']:
                    sub_categories = '반팔티'
                else:
                    sub_categories = '기타'
            elif main_categories == '스포츠':
                if sub_categories == '아우터(분류필요)':
                    main_categories = '아우터'
                    if '바람' in product_data['pageProps']['title']:
                        sub_categories = '바람막이'
                    elif '윈드' in product_data['pageProps']['title']:
                        sub_categories = '바람막이'
                    else:
                        sub_categories = '기타'
                elif sub_categories == '상의(분류필요)':
                    main_categories = '상의'
                    if '긴팔' in product_data['pageProps']['title']:
                        sub_categories = '긴팔티'
                    elif '반팔' in product_data['pageProps']['title']:
                        sub_categories = '반팔티'
                    elif '민소매' in product_data['pageProps']['title']:
                        sub_categories = '민소매'
                    elif '나시' in product_data['pageProps']['title']:
                        sub_categories = '나시'
                    elif '후드' in product_data['pageProps']['title']:
                        sub_categories = '후드'
                    else:
                        sub_categories = '기타'
                elif sub_categories == '의류(분류필요)':
                    return
                elif sub_categories == '신발(분류필요)':
                    main_categories = '신발'
                    if '러닝' in product_data['pageProps']['title']:
                        sub_categories = '러닝화/워킹화'
                    elif '워킹' in product_data['pageProps']['title']:
                        sub_categories = '러닝화/워킹화'
                    elif '축구' in product_data['pageProps']['title']:
                        sub_categories = '스포츠화'
                    elif '농구' in product_data['pageProps']['title']:
                        sub_categories = '스포츠화'
                    elif '테니스' in product_data['pageProps']['title']:
                        sub_categories = '스포츠화'
                    elif '골프' in product_data['pageProps']['title']:
                        sub_categories = '스포츠화'
                    else:
                        sub_categories = '기타'

        # 상품 정보 입력
        hiver_products[product['id']] = {
            'product_id': product['id'],
            'shopping_mall_id': 'hiver',
            'main_category': main_categories,
            'sub_category': sub_categories,
            'gender': product_data['pageProps']['fallback'][complexity]['data']['option_type'],
            'name': product_data['pageProps']['title'],
            'price': product_data['pageProps']['fallback'][complexity]['data']['sale_price'],
            'color': colors,
            'size': sizes,
            'quantity': 0, 
            'brand_name': product_data['pageProps']['fallback'][complexity]['data']['seller']['name'],
            'fee': 0,
            'image': product_data['pageProps']['image'],
            'code': None,
            'url': f'https://www.hiver.co.kr/products/{product["id"]}',
            'tags': tag_list,
            'detail_images': image_urls,
            'detail_html': image_code,
            'reviews': reviews,
        }

        print('======================================================')
        print(f'[{product["id"]}]', time.time() - start_time, '초 경과')
        print('======================================================')
        print()

        # print(hiver_products[product['id']]['detail_images'])

        # driver.quit()

        # db.products.insert_one(hiver_products[product['id']])

if __name__ == '__main__':
    # # 병렬 처리를 위한 프로세스 풀 생성
    # pool = Pool(processes=14)

    # # 대분류, 소분류, 카테고리 번호 정보를 리스트로 묶음
    # category_info_list = []
    # for main_categories in category_numbers:
    #     for sub_categories in category_numbers[main_categories]:
    #         for category_number in category_numbers[main_categories][sub_categories]:
    #             category_info_list.append((main_categories, sub_categories, category_number))

    # # 병렬 처리를 통해 각 카테고리 정보에 대해 process_category 함수를 실행
    # pool.map(hiver_process, category_info_list)

    # # 프로세스 풀 종료
    # pool.close()
    # pool.join()

    hiver_process(('상의', '아우터', 426))

    # # json 파일에 저장
    # with open('hiver_details.json', 'w', encoding='utf-8') as f:
    #     json.dump(hiver_products, f, ensure_ascii=False, indent=4)  

    print(hiver_products)