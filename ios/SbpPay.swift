@objc(SbpPay)
class SbpPay: NSObject {
    var banksList: [SBPBank]? = [];
  @objc(multiply:withB:withResolver:withRejecter:)
  func multiply(a: Float, b: Float, resolve:RCTPromiseResolveBlock,reject:RCTPromiseRejectBlock) -> Void {
    resolve(a*b)
  }
    @objc(checkUrl:withResolver:withRejecter:)
    func checkUrl(params: NSDictionary, resolve:@escaping RCTPromiseResolveBlock,reject:RCTPromiseRejectBlock) -> Void {
        let bankLoader = bankController();
        bankLoader.loadBanksList(resolve:resolve);

    }

        @objc(openSbpDeepLinkInBank:withResolver:withRejecter:)
        func openSbpDeepLinkInBank(params: NSDictionary, resolve: RCTPromiseResolveBlock,reject:RCTPromiseRejectBlock) -> Void {

            let url = URL(fileURLWithPath: params["url"] as! String);
            guard var components = URLComponents(url:  url, resolvingAgainstBaseURL: true) else {return}
            components.scheme = params["schema"] as? String
            guard let resultUrl = components.url else { return }
        UIApplication.shared.open(resultUrl, options: [:], completionHandler: nil)
        }

}

class bankController {
    var sbpURL = URL(string: "https://qr.nspk.ru/AD10001BJPGUR9P18FHR8H8QLFMLV8SH?type=02&bank=100000000111&sum=3&cur=RUB&crc=1ABE")
      var banks: [SBPBank] = [];
    func callbackAns(banks: [SBPBank]) {

        let result: NSMutableArray = []
          for bank in banks {
            let yourAuxDic: NSMutableDictionary = [:]
            yourAuxDic["name"] = bank.name
            yourAuxDic["schema"] = bank.schema
              yourAuxDic["icon"] = bank.logoURL
            result.add(yourAuxDic)
          }
        self.resolve!(result)
    }
    var resolve: RCTPromiseResolveBlock?
    func loadBanksList(resolve:@escaping RCTPromiseResolveBlock) {
        self.resolve = resolve;
           func handleBanksLoaded(banks: [SBPBank]) ->  [SBPBank] {
               let result = banks.filter { checkIfBankAppAvailable(bank: $0) }
               guard !banks.isEmpty else {
                   return []
               }

   //            guard banks.count > 1 else {
   //                openBankApplication(bank: banks[0])
   //                return []
   //            }
               return result

           }
            return loadBanks { [weak self] result in
               DispatchQueue.main.async {
                   switch result {
                   case let .success(banks):
                       DispatchQueue.main.async {
                           let B = handleBanksLoaded(banks: banks)
                           NSLog("BANKS_COUNT")
                           if(B.count > 0) {
                               self?.callbackAns(banks: B)
                           } else {

                           }
                       }
                   case .failure(_):
                       NSLog("NO")
                   }
               }
           }

       }

    func loadBanks(completion: @escaping (Result<[SBPBank], Error>) -> Void) {
           loadSBPBanks(completion: { result in
               switch result {
               case let .success(result):
                   completion(.success(result.banks))
               case let .failure(error):
                   completion(.failure(error))
               }
           })
       }


       func loadSBPBanks(completion: @escaping (Result<SBPBankResponse, Error>) -> Void) {
           let loader = DefaultSBPBankLoader()
           loader.loadBanks(completion: completion)
       }
       func checkIfBankAppAvailable(bank: SBPBank) -> Bool {
           guard let url = URL(string: "\(bank.schema)://") else { return false }
           return UIApplication.shared.canOpenURL(url)
       }

}

final class DefaultSBPBankLoader: SBPBankLoader {

    enum Error: Swift.Error {
        case failedToLoadBanksList
    }

    func loadBanks(completion: @escaping (Result<SBPBankResponse, Swift.Error>) -> Void) {
        URLSession.shared.dataTask(with: .bankListURL) { data, _, error in
            guard error == nil else {
                completion(.failure(error!))
                return
            }

            guard let data = data else {
                completion(.failure(Error.failedToLoadBanksList))
                return
            }

            let decoder = JSONDecoder()
            do {
                let response = try decoder.decode(SBPBankResponse.self, from: data)
                completion(.success(response))
            } catch {
                completion(.failure(error))
            }
        }.resume()
    }
}

private extension URL {
    static var bankListURL: URL {
        return URL(string: "https://qr.nspk.ru/proxyapp/c2bmembers.json")!
    }
}


protocol SBPBankLoader {
    func loadBanks(completion: @escaping (Result<SBPBankResponse, Swift.Error>) -> Void)
}

public struct SBPBank: Decodable {
    public let name: String
    public let logoURL: String
    public let schema: String

    enum CodingKeys: String, CodingKey {
        case name = "bankName"
        case logoURL
        case schema
    }

    public init(name: String,
                logoURL: String,
                schema: String) {
        self.name = name
        self.logoURL = logoURL
        self.schema = schema
    }
}

public struct SBPBankResponse: Decodable {
    public let banks: [SBPBank]

    enum CodingKeys: String, CodingKey {
        case banks = "dictionary"
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        var banksArray = try container.nestedUnkeyedContainer(forKey: .banks)
        var resultBanks = [SBPBank]()
        while !banksArray.isAtEnd {
            guard let bank = try? banksArray.decode(SBPBank.self) else {
                continue
            }
            resultBanks.append(bank)
        }
        self.banks = resultBanks
    }
}

