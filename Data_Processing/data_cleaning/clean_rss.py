import os
import re
import numpy as np
import pandas as pd
import manuf
def read_files(dir_path):
    files=os.listdir(dir_path)
    result=''
    for file in files:
        with open(dir_path+'/'+file,"r") as f:
            result+=f.read()
            f.close()
    return result
def get_raw_samples(raw_string):
    pattern=re.compile(r'(.*?)---',re.DOTALL)
    result=pattern.findall(raw_string)
    return result
def recompile_sample(raw_sample):
    label_pattern=re.compile(r'-\d+\s([A-I])')
    labels=label_pattern.search(raw_sample).group(1)
    RSSID_pattern=re.compile(r'(.*?)\s..:..:..:..:..:..\s-')
    RSSID = RSSID_pattern.findall(raw_sample)
    #print(RSSID)
    feature_pattern=re.compile(r'\b(..:..:..:..:..:..)\s-')
    features=feature_pattern.findall(raw_sample)
    #print(features)
    feature_values=re.findall(r'\s(-\d+)\s[A-I]\n',raw_sample)
    #print(feature_values)
    dic={"RSSID":RSSID,"features":features,"feature_values":feature_values,"labels":labels}
    return dic
def get_unique_macs(samples,filter_threshold):
    l_m=[]
    l_v=[]
    p = manuf.MacParser(update=True)
    for sample in samples:
        for each_mac in sample["features"]:
            l_m.append(each_mac)
        for each_value in sample["feature_values"]:
            l_v.append(each_value)


    macs=np.array(filter_multi_mac(l_m,l_v, 3))
    macs_uniq=np.unique(macs)
    #filter out sample frequency lower then threshold
    #get count
    macs_count=np.zeros(len(macs_uniq))
    for mac in macs:
        for i,mac_uniq in enumerate(macs_uniq):
            if mac==mac_uniq:
                macs_count[i] += 1
                if p.get_manuf(mac) == None:
                    macs_count[i]   = 0
    #filtering
    return macs_uniq[np.where(macs_count>filter_threshold)]
def rearrange_sample(macs_uniq,sample):
    raw_features = sample["features"]
    line=np.ones([1,np.size(macs_uniq,0)+1])*-100
    for i,raw_feature in enumerate(raw_features):
        for j,mac_uniq in enumerate(macs_uniq):
            if(raw_feature==mac_uniq):
                line[:,j]=sample["feature_values"][i]
    if (sample["labels"] == 'A'):
        line[:,-1]  = 0
    if (sample["labels"] == 'B'):
        line[:, -1] = 1
    if (sample["labels"] == 'C'):
        line[:, -1] = 2
    if (sample["labels"] == 'D'):
        line[:, -1] = 3
    if (sample["labels"] == 'E'):
        line[:, -1] = 4
    if (sample["labels"] == 'F'):
        line[:, -1] = 5
    if (sample["labels"] == 'G'):
        line[:, -1] = 6
    if (sample["labels"] == 'H'):
        line[:, -1] = 7
    if (sample["labels"] == 'I'):
        line[:, -1] = 8
    return line
def save2excel(data,name):
    data_pd=pd.DataFrame(data)
    writer = pd.ExcelWriter(name)
    data_pd.to_excel(writer,'page_1',float_format='%.5f')
    writer.save()
    writer.close()
def save2txt(data,name):
    np.savetxt(name,data)
def filter_samples(raw_samples,threshold):
    #filter out samples with rss below threshold
    samples_filtered=[]
    for raw_sample in raw_samples:
        sample = recompile_sample(raw_sample)
        sample_filtered = {"features": [], "feature_values": [], "labels":sample["labels"] }
        for i,value in enumerate(sample["feature_values"]):
            if(int(value)>=threshold):
                sample_filtered["features"].append(sample["features"][i])
                sample_filtered["feature_values"].append(value)

        samples_filtered.append(sample_filtered)
    return samples_filtered
def filter_multi_mac(l_m,l_v,diff):
    #filter out multiple macs from the same device if the rss difference is smaller than diff
    #sample=samples[0]
    # l_m=sample['features']
    # l_v=sample['feature_values']
    for i in range(len(l_m)):
        for j in range(i, len(l_m)):
            if (l_m[i][-2:] != l_m[j][-2:] and l_m[i][:-2] == l_m[j][:-2] and abs(
                    float(l_v[i]) - float(l_v[j])) < diff):
                # print(l_m[i])
                # print(l_v[i])
                # print(l_m[j])
                # print(l_v[j])
                if(float(l_v[i])>=float(l_v[j])):#choose the one with larger RSS
                    l_m[j] = 'delete'
                else:
                    l_m[i] = 'delete'
    #delete the redundant feature
    #dic = { "features": [], "feature_values": []}
    result=[]
    for m,v in zip(l_m,l_v):
        if m !='delete':
            result.append(m)
            #dic["feature_values"].append(v)
    return result

if __name__=="__main__":
    dir_path = "./data"
    txt=read_files(dir_path)
    raw_samples=get_raw_samples(txt)
    # print(raw_samples)
    samples=filter_samples(raw_samples,-1000)

    macs_uniq=get_unique_macs(samples,20)
    print(macs_uniq)
    print(len(macs_uniq))

    traning_data=np.empty([1,np.size(macs_uniq,0)+1])
    for i,sample in enumerate(samples):
        line=rearrange_sample(macs_uniq,sample)
        traning_data=np.vstack((traning_data,line))
    traning_data = np.delete(traning_data, obj=0, axis=0)
    #print(traning_data.shape)
    save2txt(traning_data,"training_data.txt")

    with open("all_macs.txt", 'w') as f:
        for each_row in macs_uniq:
            f.write(each_row+'\n')
        f.close()
    # #Processing testing data
    #
    raw_testing_samples=get_raw_samples(read_files("./data_testing"))
    testing_samples = filter_samples(raw_testing_samples, -1000)
    testing_data = np.empty([1, np.size(macs_uniq, 0) + 1])
    for i,testing_sample in enumerate(testing_samples):
        line=rearrange_sample(macs_uniq,testing_sample)
        testing_data=np.vstack((testing_data,line))
    testing_data = np.delete(testing_data, obj=0, axis=0)
    print(testing_data.shape)
    save2txt(testing_data,"testing_data.txt")

    #######################
    #macs_uniq=np.transpose(macs_uniq)
    #target=traning_data[:,-1]
    #indecies = [106	,99	,87,	92	,104	,36,	29	,72	,28,	19	,69	,16,	73,	98,	124	,15	,48,	88,	53	,8	,123	,32,	85,	0	,127]
    #chosen_macs =macs_uniq[indecies]
    #print(chosen_macs)
    #with open("chosen_macs.txt", 'w') as f:
    #   for each_row in chosen_macs:
    #       f.write(each_row+'\n')
    #   f.close()
    # print(traning_data.shape)
    # traning_data_selected=traning_data[:,indecies]
    # save2txt(traning_data_selected, "data_loc.txt")