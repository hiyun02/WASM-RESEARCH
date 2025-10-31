use bytecodec::DecodeExt;
use httpcodec::{HttpVersion, ReasonPhrase, Request, RequestDecoder, Response, StatusCode};
use std::io::{Read, Write};
use wasmedge_wasi_socket::{Shutdown, TcpListener, TcpStream};
use serde::{Serialize, Deserialize};
use serde_json::{Value, json};
use mysql::*;
use mysql::prelude::Queryable;

#[derive(Serialize, Deserialize)]
struct BmiDTO {
    height: String,
    weight: String,
    bmi: String,
}

fn handle_http(req: Request<String>) -> bytecodec::Result<Response<String>> {

    println!("Received from Client: {}", req.body());
    let body: Value = serde_json::from_str(req.body()).unwrap_or_default();
    let height = body["height"].as_f64().unwrap_or(0.0);
    let weight = body["weight"].as_f64().unwrap_or(0.0);
    println!("input height : {}", height);
    println!("input weight : {}", weight);
    let bmi = get_bmi(height, weight); 
    println!("calculated bmi value : {}", bmi);

    let bmiDTO = BmiDTO {
        height: height.to_string(),
        weight: weight.to_string(),
        bmi: bmi.to_string(),
    };


    let result = match insert_and_select_to_db(bmiDTO) {
        Ok(data) => {
            println!("Success to DB Insert");
            json!(data)
        },
        Err(e) => {
            eprintln!("Failed to insert and retrieve BMI data: {:?}", e);
            json!({"error" : "Failed to insert and retrieve BMI data"})
        }
    };

    Ok(Response::new(
        HttpVersion::V1_0,
        StatusCode::new(200)?,
        ReasonPhrase::new("")?,
        result.to_string(),
    ))
}



fn handle_client(mut stream: TcpStream) -> std::io::Result<()> {
    println!("Connected by Client");
    let mut buff = [0u8; 1024];
    let mut data = Vec::new();

    loop {
        let n = stream.read(&mut buff)?;
        data.extend_from_slice(&buff[0..n]);
        if n < 1024 {
            break;
        }
    }

    let mut decoder =
        RequestDecoder::<httpcodec::BodyDecoder<bytecodec::bytes::Utf8Decoder>>::default();

    let req = match decoder.decode_from_bytes(data.as_slice()) {
        Ok(req) => handle_http(req),
        Err(e) => Err(e),
    };


    let r = match req {
        Ok(r) => r,
        Err(e) => {
            let err = format!("{:?}", e);
            Response::new(
                HttpVersion::V1_0,
                StatusCode::new(500).unwrap(),
                ReasonPhrase::new(err.as_str()).unwrap(),
                err.clone(),
            )
        }
    };

    let write_buf = r.to_string();
    stream.write(write_buf.as_bytes())?;
    stream.shutdown(Shutdown::Both)?;
    Ok(())
}


fn main() -> std::io::Result<()> {
    let port = std::env::var("PORT").unwrap_or("16000".to_string());
    println!("new connection at {}", port);
    let listener = TcpListener::bind(format!("0.0.0.0:{}", port), false)?;
    loop {
        let _ = handle_client(listener.accept(false)?.0);
    }
}

fn insert_and_select_to_db(bmiDTO: BmiDTO) -> Result<Vec<BmiDTO>> {
    let url = "mysql://{user_id}:{password}@{DB_host}:3306/myDB";
    let pool = Pool::new(url)?;
    let mut conn = pool.get_conn()?;
    conn.exec_drop(
        r"INSERT INTO BMI_DATA (HEIGHT, WEIGHT, BMI) VALUES (:height, :weight, :bmi)",
        params! {
            "height" => &bmiDTO.height,
            "weight" => &bmiDTO.weight,
            "bmi" => &bmiDTO.bmi,
        },
    )?;

    let result = conn.query_map(
        "SELECT HEIGHT, WEIGHT, BMI FROM BMI_DATA",
        |(HEIGHT, WEIGHT, BMI)| BmiDTO {height: HEIGHT, weight: WEIGHT, bmi: BMI}, 
        )?;

    Ok(result)
}

fn get_bmi(height: f64, weight: f64) -> String {
    
    if height > 0.0 {
        let height_meters = height / 100.0;
        let bmi = weight / (height_meters * height_meters);
        format!("{:.2}",bmi)
    } else {
        "0.00".to_string()
    }
}

