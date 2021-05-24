function [index]=findFeatureIndex(uniqueMac,sample_mac)
    for i=1:length(uniqueMac)
        if sample_mac==uniqueMac(i)
            index=i;
        else
            index=-1;
        end
    end
end